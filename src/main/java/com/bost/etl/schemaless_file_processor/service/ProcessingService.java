package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.entity.FileUpload;
import com.bost.etl.schemaless_file_processor.entity.TemplateField;
import com.bost.etl.schemaless_file_processor.entity.UploadError;
import com.bost.etl.schemaless_file_processor.entity.UploadRecord;
import com.bost.etl.schemaless_file_processor.entity.UploadTemplate;
import com.bost.etl.schemaless_file_processor.exception.FileProcessingException;
import com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException;
import com.bost.etl.schemaless_file_processor.kafka.event.UploadCompletedEvent;
import com.bost.etl.schemaless_file_processor.kafka.producer.KafkaEventProducer;
import com.bost.etl.schemaless_file_processor.mapper.FieldMapper;
import com.bost.etl.schemaless_file_processor.metrics.FileProcessingMetrics;
import com.bost.etl.schemaless_file_processor.reader.FileReaderFactory;
import com.bost.etl.schemaless_file_processor.repository.FileUploadRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadErrorRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadRecordRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadTemplateRepository;
import com.bost.etl.schemaless_file_processor.validator.RecordValidator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProcessingService {

    private final FileReaderFactory fileReaderFactory;
    private final FieldMapper fieldMapper;
    private final RecordValidator recordValidator;
    private final FileUploadRepository fileUploadRepository;
    private final UploadTemplateRepository templateRepository;
    private final UploadRecordRepository recordRepository;
    private final UploadErrorRepository errorRepository;
    private final KafkaEventProducer kafkaEventProducer;
    private final FileProcessingMetrics metrics;

    @Value("${app.file.storage.location:./uploads}")
    private String storageLocation;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public void processFileUpload(UUID uploadId) {
        log.info("Starting processing for upload ID: {}", uploadId);
        long startTime = System.currentTimeMillis();

        FileUpload fileUpload = fileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("FileUpload", uploadId));

        if (!"PENDING".equals(fileUpload.getUploadStatus())) {
            log.warn("File upload {} is not in PENDING status, current status: {}", uploadId, fileUpload.getUploadStatus());
            return;
        }

        try {
            fileUpload.setUploadStatus("PROCESSING");
            fileUploadRepository.save(fileUpload);

            UploadTemplate template = templateRepository.findByIdWithFields(fileUpload.getTemplate().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template", fileUpload.getTemplate().getId()));

            List<TemplateField> templateFields = template.getFields();

            File file = Paths.get(storageLocation, fileUpload.getFileName()).toFile();
            if (!file.exists()) {
                throw new FileProcessingException("File not found: " + file.getAbsolutePath());
            }

            List<Map<String, String>> rows = fileReaderFactory.readRows(file, fileUpload.getFileType());
            metrics.incrementFileUploads(fileUpload.getFileType());

            int totalRecords = rows.size();
            int successfulRecords = 0;
            int failedRecords = 0;
            int currentRow = 1;

            String templateId = template.getId().toString();

            for (Map<String, String> rowData : rows) {
                try {
                    long mapStart = System.currentTimeMillis();
                    JsonNode mappedData = fieldMapper.mapRowToTemplateFields(rowData, templateFields);
                    metrics.recordMappingTime(templateId, System.currentTimeMillis() - mapStart);
                    
                    long validateStart = System.currentTimeMillis();
                    RecordValidator.ValidationResult validationResult = 
                            recordValidator.validateRecord(mappedData, templateFields);
                    metrics.recordValidationTime(templateId, System.currentTimeMillis() - validateStart);

                    if (validationResult.isValid()) {
                        UploadRecord record = UploadRecord.builder()
                                .upload(fileUpload)
                                .recordData(mappedData)
                                .validationStatus("VALID")
                                .processingStatus("PENDING")
                                .build();
                        
                        recordRepository.save(record);
                        successfulRecords++;
                        metrics.incrementRecordsValid(templateId);
                    } else {
                        UploadRecord record = UploadRecord.builder()
                                .upload(fileUpload)
                                .recordData(mappedData)
                                .validationStatus("INVALID")
                                .validationErrors(validationResult.getErrors())
                                .processingStatus("FAILED")
                                .build();
                        
                        recordRepository.save(record);

                        UploadError error = UploadError.builder()
                                .upload(fileUpload)
                                .rowNumber(currentRow)
                                .errorMessage("Validation failed: " + validationResult.getErrors().toString())
                                .rowData(mappedData)
                                .build();
                        
                        errorRepository.save(error);
                        failedRecords++;
                        metrics.incrementRecordsInvalid(templateId);
                    }
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", currentRow, e.getMessage());
                    
                    UploadError error = UploadError.builder()
                            .upload(fileUpload)
                            .rowNumber(currentRow)
                            .errorMessage("Processing error: " + e.getMessage())
                            .rowData(fieldMapper.getObjectMapper().valueToTree(rowData))
                            .build();
                    
                    errorRepository.save(error);
                    failedRecords++;
                    metrics.incrementRecordsInvalid(templateId);
                }

                currentRow++;
            }

            fileUpload.setTotalRecords(totalRecords);
            fileUpload.setSuccessfulRecords(successfulRecords);
            fileUpload.setFailedRecords(failedRecords);
            fileUpload.setUploadStatus(successfulRecords == totalRecords ? "COMPLETED" : "PARTIALLY_COMPLETED");
            fileUploadRepository.save(fileUpload);

            metrics.incrementRecordsProcessed(templateId);
            metrics.recordProcessingTime(fileUpload.getFileType(), System.currentTimeMillis() - startTime);

            if (successfulRecords == totalRecords) {
                metrics.incrementFileProcessingSuccess(fileUpload.getFileType());
            } else {
                metrics.incrementFileProcessingFailure(fileUpload.getFileType());
            }

            // Conditionally publish to Kafka based on configuration
            if (kafkaEnabled) {
                long kafkaStart = System.currentTimeMillis();
                UploadCompletedEvent event = UploadCompletedEvent.builder()
                        .uploadId(fileUpload.getId())
                        .templateId(fileUpload.getTemplate().getId())
                        .fileName(fileUpload.getFileName())
                        .uploadStatus(fileUpload.getUploadStatus())
                        .totalRecords(totalRecords)
                        .successfulRecords(successfulRecords)
                        .failedRecords(failedRecords)
                        .uploadedBy(fileUpload.getUploadedBy())
                        .completedAt(LocalDateTime.now())
                        .build();

                try {
                    kafkaEventProducer.publishUploadCompletedEvent(event);
                    metrics.recordKafkaPublishTime("upload-completed", System.currentTimeMillis() - kafkaStart);
                } catch (Exception e) {
                    log.warn("Failed to publish Kafka event, but processing completed successfully", e);
                }
            }

            log.info("Processing completed for upload ID: {}. Total: {}, Successful: {}, Failed: {}", 
                    uploadId, totalRecords, successfulRecords, failedRecords);

        } catch (Exception e) {
            log.error("Error processing file upload {}: {}", uploadId, e.getMessage(), e);
            fileUpload.setUploadStatus("FAILED");
            fileUploadRepository.save(fileUpload);
            throw new FileProcessingException("Failed to process file upload", e);
        }
    }

    public void processAllPendingUploads() {
        log.info("Processing all pending uploads");
        
        List<FileUpload> pendingUploads = fileUploadRepository.findByUploadStatus("PENDING");
        
        for (FileUpload upload : pendingUploads) {
            try {
                processFileUpload(upload.getId());
            } catch (Exception e) {
                log.error("Failed to process upload {}: {}", upload.getId(), e.getMessage());
            }
        }
        
        log.info("Completed processing {} pending uploads", pendingUploads.size());
    }
}

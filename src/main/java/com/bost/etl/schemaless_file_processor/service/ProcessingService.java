package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.entity.*;
import com.bost.etl.schemaless_file_processor.exception.FileProcessingException;
import com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException;
import com.bost.etl.schemaless_file_processor.kafka.event.UploadCompletedEvent;
import com.bost.etl.schemaless_file_processor.kafka.producer.KafkaEventProducer;
import com.bost.etl.schemaless_file_processor.mapper.FieldMapper;
import com.bost.etl.schemaless_file_processor.metrics.FileProcessingMetrics;
import com.bost.etl.schemaless_file_processor.reader.FileReaderFactory;
import com.bost.etl.schemaless_file_processor.repository.*;
import com.bost.etl.schemaless_file_processor.validator.RecordValidator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
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

@Service @RequiredArgsConstructor @Slf4j
public class ProcessingService {
    private final FileReaderFactory fileReaderFactory; private final FieldMapper fieldMapper; private final RecordValidator recordValidator;
    private final FileUploadRepository fileUploadRepository; private final UploadTemplateRepository templateRepository;
    private final KafkaEventProducer kafkaEventProducer; private final FileProcessingMetrics metrics; private final EntityManager entityManager;
    @Value("${app.file.storage.location:./uploads}") private String storageLocation;
    @Value("${app.kafka.enabled:true}") private boolean kafkaEnabled;
    @Value("${app.file.processing.batch-size:250}") private int batchSize;

    @Transactional
    public void processFileUpload(UUID uploadId) {
        FileUpload upload=fileUploadRepository.findById(uploadId).orElseThrow(()->new ResourceNotFoundException("FileUpload",uploadId));
        if (!"PENDING".equals(upload.getUploadStatus())) { log.info("Skipping upload {} with status {}",uploadId,upload.getUploadStatus()); return; }
        upload.setUploadStatus("PROCESSING"); fileUploadRepository.save(upload);
        long started=System.currentTimeMillis(); Counters counters=new Counters();
        try {
            UploadTemplate template=templateRepository.findByIdWithFields(upload.getTemplate().getId()).orElseThrow(()->new ResourceNotFoundException("Template",upload.getTemplate().getId()));
            List<TemplateField> fields=template.getFields(); String templateId=template.getId().toString();
            File file=Paths.get(storageLocation,upload.getFileName()).toFile(); if(!file.isFile()) throw new FileProcessingException("File not found: "+file.getAbsolutePath());
            final FileUpload[] current={upload};
            fileReaderFactory.forEachRow(file, upload.getFileType(), row -> {
                counters.total++; processRow(current[0], row, fields, templateId, counters);
                if (counters.total % batchSize == 0) { entityManager.flush(); entityManager.clear(); current[0]=entityManager.getReference(FileUpload.class,uploadId); }
            });
            entityManager.flush(); entityManager.clear();
            complete(uploadId,counters,template,started);
        } catch (Exception exception) { fail(uploadId, exception); log.error("Processing failed for upload {}", uploadId, exception); }
    }

    private void processRow(FileUpload upload, Map<String,String> row, List<TemplateField> fields, String templateId, Counters counts) {
        try {
            long started=System.currentTimeMillis(); JsonNode mapped=fieldMapper.mapRowToTemplateFields(row,fields); metrics.recordMappingTime(templateId,System.currentTimeMillis()-started);
            started=System.currentTimeMillis(); RecordValidator.ValidationResult validation=recordValidator.validateRecord(mapped,fields); metrics.recordValidationTime(templateId,System.currentTimeMillis()-started);
            if(validation.isValid()) { entityManager.persist(UploadRecord.builder().upload(upload).recordData(mapped).validationStatus("VALID").processingStatus("PENDING").build()); counts.valid++; metrics.incrementRecordsValid(templateId); }
            else { entityManager.persist(UploadRecord.builder().upload(upload).recordData(mapped).validationStatus("INVALID").validationErrors(validation.getErrors()).processingStatus("FAILED").build()); entityManager.persist(UploadError.builder().upload(upload).rowNumber(counts.total).errorMessage("Validation failed: "+validation.getErrors()).rowData(mapped).build()); counts.invalid++; metrics.incrementRecordsInvalid(templateId); }
        } catch(Exception exception) { entityManager.persist(UploadError.builder().upload(upload).rowNumber(counts.total).errorMessage("Processing error: "+exception.getMessage()).rowData(fieldMapper.getObjectMapper().valueToTree(row)).build()); counts.invalid++; metrics.incrementRecordsInvalid(templateId); log.warn("Row {} failed for upload {}",counts.total,upload.getId(),exception); }
    }
    private void complete(UUID id,Counters c,UploadTemplate template,long started) { FileUpload upload=fileUploadRepository.findById(id).orElseThrow(); upload.setTotalRecords(c.total);upload.setSuccessfulRecords(c.valid);upload.setFailedRecords(c.invalid);upload.setUploadStatus(c.valid==c.total?"COMPLETED":"PARTIALLY_COMPLETED");fileUploadRepository.save(upload);metrics.incrementRecordsProcessed(template.getId().toString());metrics.recordProcessingTime(upload.getFileType(),System.currentTimeMillis()-started);if(kafkaEnabled) try { kafkaEventProducer.publishUploadCompletedEvent(UploadCompletedEvent.builder().uploadId(id).templateId(template.getId()).fileName(upload.getFileName()).uploadStatus(upload.getUploadStatus()).totalRecords(c.total).successfulRecords(c.valid).failedRecords(c.invalid).uploadedBy(upload.getUploadedBy()).completedAt(LocalDateTime.now()).build()); } catch(Exception e){log.warn("Upload completed but Kafka publication failed for {}",id,e);} }
    private void fail(UUID id,Exception exception) { try { FileUpload upload=fileUploadRepository.findById(id).orElse(null);if(upload!=null){upload.setUploadStatus("FAILED");fileUploadRepository.save(upload);} } catch(Exception ignored){log.error("Unable to mark upload {} as FAILED",id,ignored);} }
    public void processAllPendingUploads(){fileUploadRepository.findByUploadStatus("PENDING").forEach(upload->{try{processFileUpload(upload.getId());}catch(Exception e){log.error("Failed pending upload {}",upload.getId(),e);}});}
    private static final class Counters { int total; int valid; int invalid; }
}

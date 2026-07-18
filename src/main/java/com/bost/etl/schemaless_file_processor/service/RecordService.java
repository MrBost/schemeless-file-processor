package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.dto.RecordResponse;
import com.bost.etl.schemaless_file_processor.entity.FileUpload;
import com.bost.etl.schemaless_file_processor.entity.UploadRecord;
import com.bost.etl.schemaless_file_processor.exception.AccessDeniedException;
import com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException;
import com.bost.etl.schemaless_file_processor.repository.FileUploadRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.bost.etl.schemaless_file_processor.security.UserContext.getCurrentUsername;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecordService {

    private final UploadRecordRepository recordRepository;
    private final FileUploadRepository fileUploadRepository;

    public List<RecordResponse> getRecordsByUploadId(UUID uploadId) {
        validateUploadAccess(uploadId);
        return recordRepository.findByUploadId(uploadId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Page<RecordResponse> getRecordsByUploadId(UUID uploadId, Pageable pageable) {
        validateUploadAccess(uploadId);
        return recordRepository.findByUploadId(uploadId, pageable)
                .map(this::mapToResponse);
    }

    public List<RecordResponse> getValidRecordsByUploadId(UUID uploadId) {
        validateUploadAccess(uploadId);
        return recordRepository.findByUploadIdAndValidationStatus(uploadId, "VALID").stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<RecordResponse> getInvalidRecordsByUploadId(UUID uploadId) {
        validateUploadAccess(uploadId);
        return recordRepository.findByUploadIdAndValidationStatus(uploadId, "INVALID").stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<RecordResponse> getRecordsByUploadIdAndStatus(UUID uploadId, String validationStatus) {
        validateUploadAccess(uploadId);
        return recordRepository.findByUploadIdAndValidationStatus(uploadId, validationStatus).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<RecordResponse> getRecordsByJsonbField(UUID uploadId, String key, String value) {
        validateUploadAccess(uploadId);
        return recordRepository.findByUploadIdAndJsonbField(uploadId, key, value).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<RecordResponse> getRecordsByJsonbCriteria(UUID uploadId, String jsonbCriteria) {
        validateUploadAccess(uploadId);
        return recordRepository.findByUploadIdAndJsonbCriteria(uploadId, jsonbCriteria).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public long countRecordsByUploadId(UUID uploadId) {
        validateUploadAccess(uploadId);
        return recordRepository.countByUploadId(uploadId);
    }

    public long countValidRecordsByUploadId(UUID uploadId) {
        validateUploadAccess(uploadId);
        return recordRepository.countValidByUploadId(uploadId);
    }

    public long countInvalidRecordsByUploadId(UUID uploadId) {
        validateUploadAccess(uploadId);
        return recordRepository.countInvalidByUploadId(uploadId);
    }

    private void validateUploadAccess(UUID uploadId) {
        String currentUser = getCurrentUsername();
        FileUpload fileUpload = fileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("FileUpload", uploadId));
        
        if (!fileUpload.getUploadedBy().equals(currentUser)) {
            throw new AccessDeniedException("You can only access records from your own uploads");
        }
    }

    private RecordResponse mapToResponse(UploadRecord record) {
        return RecordResponse.builder()
                .recordId(record.getId())
                .uploadId(record.getUpload().getId())
                .recordData(record.getRecordData())
                .validationStatus(record.getValidationStatus())
                .validationErrors(record.getValidationErrors())
                .processingStatus(record.getProcessingStatus())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
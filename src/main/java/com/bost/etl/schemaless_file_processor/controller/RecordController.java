package com.bost.etl.schemaless_file_processor.controller;

import com.bost.etl.schemaless_file_processor.dto.RecordResponse;
import com.bost.etl.schemaless_file_processor.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
@Tag(name = "Record Management", description = "APIs for querying processed records")
@SecurityRequirement(name = "bearerAuth")
public class RecordController {

    private final RecordService recordService;

    @GetMapping("/upload/{uploadId}")
    @Operation(summary = "Get all records for an upload", description = "Retrieve all processed records for a specific file upload")
    public ResponseEntity<List<RecordResponse>> getRecordsByUploadId(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId) {
        List<RecordResponse> records = recordService.getRecordsByUploadId(uploadId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/upload/{uploadId}/page")
    @Operation(summary = "Get records for an upload (paginated)", description = "Retrieve processed records for a specific file upload with pagination")
    public ResponseEntity<Page<RecordResponse>> getRecordsByUploadId(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId,
            @Parameter(description = "Pagination parameters") @PageableDefault Pageable pageable) {
        Page<RecordResponse> records = recordService.getRecordsByUploadId(uploadId, pageable);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/upload/{uploadId}/valid")
    @Operation(summary = "Get valid records for an upload", description = "Retrieve only valid records for a specific file upload")
    public ResponseEntity<List<RecordResponse>> getValidRecordsByUploadId(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId) {
        List<RecordResponse> records = recordService.getValidRecordsByUploadId(uploadId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/upload/{uploadId}/invalid")
    @Operation(summary = "Get invalid records for an upload", description = "Retrieve only invalid records for a specific file upload")
    public ResponseEntity<List<RecordResponse>> getInvalidRecordsByUploadId(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId) {
        List<RecordResponse> records = recordService.getInvalidRecordsByUploadId(uploadId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/upload/{uploadId}/status/{status}")
    @Operation(summary = "Get records by validation status", description = "Retrieve records filtered by validation status")
    public ResponseEntity<List<RecordResponse>> getRecordsByStatus(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId,
            @Parameter(description = "Validation status (VALID, INVALID, PENDING)") @PathVariable String status) {
        List<RecordResponse> records = recordService.getRecordsByUploadIdAndStatus(uploadId, status);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/upload/{uploadId}/field/{fieldName}")
    @Operation(summary = "Get records by JSONB field", description = "Retrieve records filtered by a specific JSONB field value")
    public ResponseEntity<List<RecordResponse>> getRecordsByField(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId,
            @Parameter(description = "Field name to filter by") @PathVariable String fieldName,
            @Parameter(description = "Field value to match") @RequestParam String value) {
        List<RecordResponse> records = recordService.getRecordsByJsonbField(uploadId, fieldName, value);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/upload/{uploadId}/search")
    @Operation(summary = "Search records with JSONB criteria", description = "Retrieve records matching JSONB criteria (e.g., {\"name\":\"John\",\"age\":30})")
    public ResponseEntity<List<RecordResponse>> searchRecords(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId,
            @Parameter(description = "JSONB criteria for filtering") @RequestParam String criteria) {
        List<RecordResponse> records = recordService.getRecordsByJsonbCriteria(uploadId, criteria);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/upload/{uploadId}/count")
    @Operation(summary = "Count records for an upload", description = "Get total count of records for a specific file upload")
    public ResponseEntity<Long> countRecords(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId) {
        long count = recordService.countRecordsByUploadId(uploadId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/upload/{uploadId}/count/valid")
    @Operation(summary = "Count valid records for an upload", description = "Get count of valid records for a specific file upload")
    public ResponseEntity<Long> countValidRecords(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId) {
        long count = recordService.countValidRecordsByUploadId(uploadId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/upload/{uploadId}/count/invalid")
    @Operation(summary = "Count invalid records for an upload", description = "Get count of invalid records for a specific file upload")
    public ResponseEntity<Long> countInvalidRecords(
            @Parameter(description = "Upload ID") @PathVariable UUID uploadId) {
        long count = recordService.countInvalidRecordsByUploadId(uploadId);
        return ResponseEntity.ok(count);
    }
}
package com.bost.etl.schemaless_file_processor.controller;

import com.bost.etl.schemaless_file_processor.dto.FileUploadResponse;
import com.bost.etl.schemaless_file_processor.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.bost.etl.schemaless_file_processor.security.UserContext.getCurrentUsername;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "APIs for file upload management")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file", description = "Upload a CSV or Excel file for processing")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "File to upload") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Template ID to use for processing") @RequestParam("templateId") UUID templateId) {
        String currentUser = getCurrentUsername();
        FileUploadResponse response = fileUploadService.uploadFile(file, templateId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{uploadId}")
    @Operation(summary = "Get upload by ID", description = "Retrieve file upload details by upload ID")
    public ResponseEntity<FileUploadResponse> getUploadById(@PathVariable UUID uploadId) {
        FileUploadResponse response = fileUploadService.getUploadById(uploadId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/template/{templateId}")
    @Operation(summary = "Get uploads by template", description = "Retrieve all file uploads for a specific template")
    public ResponseEntity<List<FileUploadResponse>> getUploadsByTemplate(@PathVariable UUID templateId) {
        List<FileUploadResponse> responses = fileUploadService.getUploadsByTemplate(templateId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get uploads by user", description = "Retrieve all file uploads by a specific user")
    public ResponseEntity<List<FileUploadResponse>> getUploadsByUser(@PathVariable String userId) {
        List<FileUploadResponse> responses = fileUploadService.getUploadsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get uploads by status", description = "Retrieve all file uploads with a specific status")
    public ResponseEntity<List<FileUploadResponse>> getUploadsByStatus(@PathVariable String status) {
        List<FileUploadResponse> responses = fileUploadService.getUploadsByStatus(status);
        return ResponseEntity.ok(responses);
    }

}
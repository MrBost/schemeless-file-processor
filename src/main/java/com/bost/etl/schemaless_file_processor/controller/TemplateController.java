package com.bost.etl.schemaless_file_processor.controller;

import com.bost.etl.schemaless_file_processor.dto.TemplateCreateRequest;
import com.bost.etl.schemaless_file_processor.dto.TemplateResponse;
import com.bost.etl.schemaless_file_processor.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.bost.etl.schemaless_file_processor.security.UserContext.getCurrentUsername;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
@Tag(name = "Template Management", description = "APIs for managing upload templates")
@SecurityRequirement(name = "bearerAuth")
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @Operation(summary = "Create a new template", description = "Create a new upload template with field definitions")
    public ResponseEntity<TemplateResponse> createTemplate(
            @Valid @RequestBody TemplateCreateRequest request) {
        String currentUser = getCurrentUsername();
        TemplateResponse response = templateService.createTemplate(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Retrieve a template by its ID with all field definitions")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable UUID id) {
        TemplateResponse response = templateService.getTemplateById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all templates", description = "Retrieve all templates created by the authenticated user")
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        List<TemplateResponse> responses = templateService.getAllTemplates();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/creator/{userId}")
    @Operation(summary = "Get templates by creator", description = "Retrieve all templates created by a specific user (only your own templates)")
    public ResponseEntity<List<TemplateResponse>> getTemplatesByCreator(@PathVariable String userId) {
        List<TemplateResponse> responses = templateService.getTemplatesByCreator(userId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Update an existing template and its field definitions")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateCreateRequest request) {
        TemplateResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", description = "Delete a template by its ID")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

}
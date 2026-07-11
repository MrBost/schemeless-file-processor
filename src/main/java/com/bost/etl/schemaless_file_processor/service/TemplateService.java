package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.dto.TemplateCreateRequest;
import com.bost.etl.schemaless_file_processor.dto.TemplateFieldRequest;
import com.bost.etl.schemaless_file_processor.dto.TemplateFieldResponse;
import com.bost.etl.schemaless_file_processor.dto.TemplateResponse;
import com.bost.etl.schemaless_file_processor.entity.TemplateField;
import com.bost.etl.schemaless_file_processor.entity.UploadTemplate;
import com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException;
import com.bost.etl.schemaless_file_processor.repository.TemplateFieldRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final UploadTemplateRepository templateRepository;
    private final TemplateFieldRepository fieldRepository;

    public TemplateResponse createTemplate(TemplateCreateRequest request, String createdBy) {
        if (templateRepository.existsByTemplateName(request.getTemplateName())) {
            throw new IllegalArgumentException("Template name already exists: " + request.getTemplateName());
        }

        UploadTemplate template = UploadTemplate.builder()
                .templateName(request.getTemplateName())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();

        UploadTemplate savedTemplate = templateRepository.save(template);

        List<TemplateField> fields = request.getFields().stream()
                .map(fieldRequest -> createField(fieldRequest, savedTemplate))
                .collect(Collectors.toList());

        savedTemplate.setFields(fields);

        return mapToResponse(savedTemplate);
    }

    public TemplateResponse getTemplateById(UUID id) {
        UploadTemplate template = templateRepository.findByIdWithFields(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id));
        return mapToResponse(template);
    }

    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TemplateResponse> getTemplatesByCreator(String createdBy) {
        return templateRepository.findByCreatedBy(createdBy).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TemplateResponse updateTemplate(UUID id, TemplateCreateRequest request) {
        UploadTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id));

        if (!template.getTemplateName().equals(request.getTemplateName()) &&
                templateRepository.existsByTemplateName(request.getTemplateName())) {
            throw new IllegalArgumentException("Template name already exists: " + request.getTemplateName());
        }

        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());

        fieldRepository.deleteByTemplateId(id);

        List<TemplateField> fields = request.getFields().stream()
                .map(fieldRequest -> createField(fieldRequest, template))
                .collect(Collectors.toList());

        template.setFields(fields);
        template = templateRepository.save(template);

        return mapToResponse(template);
    }

    public void deleteTemplate(UUID id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Template", id);
        }
        templateRepository.deleteById(id);
    }

    private TemplateField createField(TemplateFieldRequest request, UploadTemplate template) {
        return TemplateField.builder()
                .template(template)
                .fieldName(request.getFieldName())
                .fieldType(request.getFieldType())
                .required(request.getRequired())
                .validationRule(request.getValidationRule())
                .build();
    }

    private TemplateResponse mapToResponse(UploadTemplate template) {
        List<TemplateFieldResponse> fieldResponses = template.getFields().stream()
                .map(this::mapFieldToResponse)
                .collect(Collectors.toList());

        return TemplateResponse.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .fields(fieldResponses)
                .build();
    }

    private TemplateFieldResponse mapFieldToResponse(TemplateField field) {
        return TemplateFieldResponse.builder()
                .id(field.getId())
                .templateId(field.getTemplate() != null ? field.getTemplate().getId() : null)
                .fieldName(field.getFieldName())
                .fieldType(field.getFieldType())
                .required(field.getRequired())
                .validationRule(field.getValidationRule())
                .createdAt(field.getCreatedAt())
                .build();
    }
}

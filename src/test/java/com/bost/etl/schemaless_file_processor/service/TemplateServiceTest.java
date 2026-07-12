package com.bost.etl.schemaless_file_processor.service;

import com.bost.etl.schemaless_file_processor.dto.TemplateCreateRequest;
import com.bost.etl.schemaless_file_processor.dto.TemplateFieldRequest;
import com.bost.etl.schemaless_file_processor.entity.TemplateField;
import com.bost.etl.schemaless_file_processor.entity.UploadTemplate;
import com.bost.etl.schemaless_file_processor.repository.TemplateFieldRepository;
import com.bost.etl.schemaless_file_processor.repository.UploadTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private UploadTemplateRepository templateRepository;

    @Mock
    private TemplateFieldRepository fieldRepository;

    @InjectMocks
    private TemplateService templateService;

    private TemplateCreateRequest templateRequest;
    private UploadTemplate template;

    @BeforeEach
    void setUp() {
        TemplateFieldRequest fieldRequest1 = TemplateFieldRequest.builder()
                .fieldName("name")
                .fieldType("string")
                .required(true)
                .validationRule("minLength:2")
                .build();

        TemplateFieldRequest fieldRequest2 = TemplateFieldRequest.builder()
                .fieldName("age")
                .fieldType("integer")
                .required(true)
                .validationRule("min:0")
                .build();

        templateRequest = TemplateCreateRequest.builder()
                .templateName("customer_template")
                .description("Customer data template")
                .fields(Arrays.asList(fieldRequest1, fieldRequest2))
                .build();

        template = UploadTemplate.builder()
                .id(UUID.randomUUID())
                .templateName("customer_template")
                .description("Customer data template")
                .createdBy("test_user")
                .build();
    }

    @Test
    void createTemplate_Success() {
        when(templateRepository.existsByTemplateName(anyString())).thenReturn(false);
        when(templateRepository.save(any(UploadTemplate.class))).thenReturn(template);
        when(fieldRepository.saveAll(anyList())).thenReturn(Arrays.asList());

        var response = templateService.createTemplate(templateRequest, "test_user");

        assertNotNull(response);
        assertEquals("customer_template", response.getTemplateName());
        verify(templateRepository).save(any(UploadTemplate.class));
        verify(fieldRepository).saveAll(anyList());
    }

    @Test
    void createTemplate_DuplicateName_ThrowsException() {
        when(templateRepository.existsByTemplateName(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> templateService.createTemplate(templateRequest, "test_user"));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void getTemplateById_Success() {
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findByIdWithFields(templateId)).thenReturn(Optional.of(template));

        var response = templateService.getTemplateById(templateId);

        assertNotNull(response);
        assertEquals("customer_template", response.getTemplateName());
        verify(templateRepository).findByIdWithFields(templateId);
    }

    @Test
    void getTemplateById_NotFound_ThrowsException() {
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findByIdWithFields(templateId)).thenReturn(Optional.empty());

        assertThrows(com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException.class, 
                () -> templateService.getTemplateById(templateId));
    }

    @Test
    void getAllTemplates_Success() {
        when(templateRepository.findAll()).thenReturn(Arrays.asList(template));

        var templates = templateService.getAllTemplates();

        assertNotNull(templates);
        assertEquals(1, templates.size());
        verify(templateRepository).findAll();
    }

    @Test
    void deleteTemplate_Success() {
        UUID templateId = UUID.randomUUID();
        when(templateRepository.existsById(templateId)).thenReturn(true);

        templateService.deleteTemplate(templateId);

        verify(templateRepository).deleteById(templateId);
    }

    @Test
    void deleteTemplate_NotFound_ThrowsException() {
        UUID templateId = UUID.randomUUID();
        when(templateRepository.existsById(templateId)).thenReturn(false);

        assertThrows(com.bost.etl.schemaless_file_processor.exception.ResourceNotFoundException.class,
                () -> templateService.deleteTemplate(templateId));
        verify(templateRepository, never()).deleteById(any());
    }
}

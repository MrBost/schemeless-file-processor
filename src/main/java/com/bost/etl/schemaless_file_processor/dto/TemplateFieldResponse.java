package com.bost.etl.schemaless_file_processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateFieldResponse {

    private UUID id;
    private UUID templateId;
    private String fieldName;
    private String fieldType;
    private Boolean required;
    private String validationRule;
    private LocalDateTime createdAt;
}

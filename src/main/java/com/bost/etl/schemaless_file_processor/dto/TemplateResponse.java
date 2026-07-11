package com.bost.etl.schemaless_file_processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {

    private UUID id;
    private String templateName;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<TemplateFieldResponse> fields;
}

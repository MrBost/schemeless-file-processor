package com.bost.etl.schemaless_file_processor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateFieldRequest {

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotBlank(message = "Field type is required")
    private String fieldType;

    @NotNull(message = "Required flag is required")
    private Boolean required;

    private String validationRule;
}

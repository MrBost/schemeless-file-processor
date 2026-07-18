package com.bost.etl.schemaless_file_processor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordResponse {

    private UUID recordId;
    private UUID uploadId;
    private JsonNode recordData;
    private String validationStatus;
    private JsonNode validationErrors;
    private String processingStatus;
    private LocalDateTime createdAt;
}
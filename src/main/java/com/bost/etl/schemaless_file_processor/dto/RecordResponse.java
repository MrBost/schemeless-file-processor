package com.bost.etl.schemaless_file_processor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private UUID recordId;
    private UUID uploadId;
    private Object recordData;
    private String validationStatus;
    private Object validationErrors;
    private String processingStatus;
    private LocalDateTime createdAt;

    // Custom setter to convert JsonNode to Object for proper serialization
    public void setRecordData(JsonNode recordData) {
        this.recordData = convertJsonNode(recordData);
    }

    public void setValidationErrors(JsonNode validationErrors) {
        this.validationErrors = convertJsonNode(validationErrors);
    }

    private Object convertJsonNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.treeToValue(node, Object.class);
        } catch (JsonProcessingException e) {
            return node.toString();
        }
    }
}
package com.bost.etl.schemaless_file_processor.mapper;

import com.bost.etl.schemaless_file_processor.entity.TemplateField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Component
@RequiredArgsConstructor
@Slf4j
public class FieldMapper {

    private final ObjectMapper objectMapper;

    public JsonNode mapRowToTemplateFields(Map<String, String> rowData, List<TemplateField> templateFields) {
        ObjectNode recordNode = objectMapper.createObjectNode();

        for (TemplateField field : templateFields) {
            String fieldName = field.getFieldName();
            String fieldType = field.getFieldType();
            String value = rowData.getOrDefault(fieldName, "");

            Object mappedValue = mapValueToType(value, fieldType);
            recordNode.putPOJO(fieldName, mappedValue);
        }

        log.debug("Mapped row to JSON: {}", recordNode);
        return recordNode;
    }

    private Object mapValueToType(String value, String fieldType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return switch (fieldType.toLowerCase()) {
            case "string", "text" -> value.trim();
            case "integer", "int" -> {
                try {
                    yield Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse integer from value: {}", value);
                    yield value.trim();
                }
            }
            case "long", "bigint" -> {
                try {
                    yield Long.parseLong(value.trim());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse long from value: {}", value);
                    yield value.trim();
                }
            }
            case "double", "decimal", "float" -> {
                try {
                    yield Double.parseDouble(value.trim());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse double from value: {}", value);
                    yield value.trim();
                }
            }
            case "boolean" -> {
                String normalized = value.trim().toLowerCase();
                yield normalized.equals("true") || normalized.equals("1") || normalized.equals("yes");
            }
            case "date" -> value.trim();
            case "datetime", "timestamp" -> value.trim();
            case "json" -> {
                try {
                    yield objectMapper.readTree(value.trim());
                } catch (Exception e) {
                    log.warn("Failed to parse JSON from value: {}", value);
                    yield value.trim();
                }
            }
            default -> value.trim();
        };
    }
}

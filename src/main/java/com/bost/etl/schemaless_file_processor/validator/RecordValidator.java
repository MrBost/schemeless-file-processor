package com.bost.etl.schemaless_file_processor.validator;

import com.bost.etl.schemaless_file_processor.entity.TemplateField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordValidator {

    private final ObjectMapper objectMapper;

    public ValidationResult validateRecord(JsonNode recordData, List<TemplateField> templateFields) {
        ValidationResult result = new ValidationResult();
        ArrayNode errors = objectMapper.createArrayNode();

        for (TemplateField field : templateFields) {
            String fieldName = field.getFieldName();
            String fieldType = field.getFieldType();
            Boolean required = field.getRequired();
            String validationRule = field.getValidationRule();

            JsonNode fieldValue = recordData.get(fieldName);

            if (required && (fieldValue == null || fieldValue.isNull() || fieldValue.asText().isEmpty())) {
                errors.add("Field '" + fieldName + "' is required but missing or empty");
                continue;
            }

            if (fieldValue != null && !fieldValue.isNull() && !fieldValue.asText().isEmpty()) {
                validateFieldType(fieldName, fieldValue, fieldType, errors);
                validateCustomRule(fieldName, fieldValue, validationRule, errors);
            }
        }

        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        
        if (!result.isValid()) {
            log.debug("Validation failed for record with {} errors", errors.size());
        }

        return result;
    }

    private void validateFieldType(String fieldName, JsonNode fieldValue, String fieldType, ArrayNode errors) {
        String value = fieldValue.asText();

        try {
            switch (fieldType.toLowerCase()) {
                case "integer", "int" -> {
                    Integer.parseInt(value);
                }
                case "long", "bigint" -> {
                    Long.parseLong(value);
                }
                case "double", "decimal", "float" -> {
                    Double.parseDouble(value);
                }
                case "boolean" -> {
                    String normalized = value.toLowerCase();
                    if (!normalized.equals("true") && !normalized.equals("false") &&
                        !normalized.equals("1") && !normalized.equals("0") &&
                        !normalized.equals("yes") && !normalized.equals("no")) {
                        errors.add("Field '" + fieldName + "' must be a boolean value");
                    }
                }
            }
        } catch (NumberFormatException e) {
            errors.add("Field '" + fieldName + "' must be a valid " + fieldType);
        }
    }

    private void validateCustomRule(String fieldName, JsonNode fieldValue, String validationRule, ArrayNode errors) {
        if (validationRule == null || validationRule.trim().isEmpty()) {
            return;
        }

        String value = fieldValue.asText();

        if (validationRule.startsWith("regex:")) {
            String pattern = validationRule.substring(6);
            try {
                if (!Pattern.matches(pattern, value)) {
                    errors.add("Field '" + fieldName + "' does not match the required pattern");
                }
            } catch (Exception e) {
                log.warn("Invalid regex pattern for field '{}': {}", fieldName, pattern);
            }
        } else if (validationRule.startsWith("min:")) {
            try {
                double minValue = Double.parseDouble(validationRule.substring(4));
                double numericValue = Double.parseDouble(value);
                if (numericValue < minValue) {
                    errors.add("Field '" + fieldName + "' must be at least " + minValue);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid min value for field '{}': {}", fieldName, validationRule);
            }
        } else if (validationRule.startsWith("max:")) {
            try {
                double maxValue = Double.parseDouble(validationRule.substring(4));
                double numericValue = Double.parseDouble(value);
                if (numericValue > maxValue) {
                    errors.add("Field '" + fieldName + "' must be at most " + maxValue);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid max value for field '{}': {}", fieldName, validationRule);
            }
        } else if (validationRule.startsWith("minLength:")) {
            try {
                int minLength = Integer.parseInt(validationRule.substring(10));
                if (value.length() < minLength) {
                    errors.add("Field '" + fieldName + "' must be at least " + minLength + " characters");
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid minLength for field '{}': {}", fieldName, validationRule);
            }
        } else if (validationRule.startsWith("maxLength:")) {
            try {
                int maxLength = Integer.parseInt(validationRule.substring(10));
                if (value.length() > maxLength) {
                    errors.add("Field '" + fieldName + "' must be at most " + maxLength + " characters");
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid maxLength for field '{}': {}", fieldName, validationRule);
            }
        } else if (validationRule.equals("email")) {
            String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            if (!Pattern.matches(emailPattern, value)) {
                errors.add("Field '" + fieldName + "' must be a valid email address");
            }
        } else if (validationRule.equals("url")) {
            try {
                new java.net.URL(value);
            } catch (Exception e) {
                errors.add("Field '" + fieldName + "' must be a valid URL");
            }
        }
    }

    @Setter
    @Getter
    public static class ValidationResult {
        private boolean valid;
        private JsonNode errors;

    }
}

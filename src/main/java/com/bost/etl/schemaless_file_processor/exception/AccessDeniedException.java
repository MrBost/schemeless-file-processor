package com.bost.etl.schemaless_file_processor.exception;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String resourceName, String action) {
        super(String.format("You do not have permission to %s this %s", action, resourceName));
    }
}
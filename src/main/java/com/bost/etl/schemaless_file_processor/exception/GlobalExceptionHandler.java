package com.bost.etl.schemaless_file_processor.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabledException(DisabledException ex) {
        log.warn("Account disabled: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.FORBIDDEN, "Account is disabled");
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLockedException(LockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.FORBIDDEN, "Account is locked");
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ProblemDetail handleAccountExpiredException(AccountExpiredException ex) {
        log.warn("Account expired: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.FORBIDDEN, "Account is expired");
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ProblemDetail handleInsufficientAuthenticationException(InsufficientAuthenticationException ex) {
        log.warn("Insufficient authentication: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ProblemDetail handleAuthenticationServiceException(AuthenticationServiceException ex) {
        log.warn("Authentication service error: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleSpringAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(com.bost.etl.schemaless_file_processor.exception.AccessDeniedException.class)
    public ProblemDetail handleCustomAccessDeniedException(com.bost.etl.schemaless_file_processor.exception.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(FileProcessingException.class)
    public ProblemDetail handleFileProcessingException(FileProcessingException ex) {
        log.warn("File processing error: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid input: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        log.warn("Validation failed: {}", errors);
        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        return problemDetail;
    }
}
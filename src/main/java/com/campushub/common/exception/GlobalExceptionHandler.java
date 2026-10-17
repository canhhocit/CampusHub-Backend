package com.campushub.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, String>> handleAppException(AppException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getErrorCode().name());
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthenticationException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "UNAUTHORIZED");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "FORBIDDEN");
        body.put("message", "Access Denied");
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
        org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        org.springframework.dao.OptimisticLockingFailureException.class,
        jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<Map<String, String>> handleOptimisticLockingFailure(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ErrorCode.CONCURRENT_UPDATE.name());
        body.put("message", ErrorCode.CONCURRENT_UPDATE.getMessage());
        return new ResponseEntity<>(body, ErrorCode.CONCURRENT_UPDATE.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
        ex.printStackTrace();
        Map<String, String> body = new HashMap<>();
        body.put("error", "INTERNAL_SERVER_ERROR");
        body.put("message", "An unexpected error occurred. Please try again later.");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", "INVALID_REQUEST",
                "message", ex.getReason() == null ? "Invalid request" : ex.getReason()));
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> handleMalformedRequest(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST", "message", "Invalid request value"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "RESOURCE_CONFLICT",
                "message", "Resource conflict. Refresh the page and try again."));
    }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleLockConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CONCURRENT_UPDATE",
                "message", "Resource is being updated. Please retry."));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadSize(Exception ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "FILE_TOO_LARGE",
                "message", "Image must be at most 10 MB"));
    }
}

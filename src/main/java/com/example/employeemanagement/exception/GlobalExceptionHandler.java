package com.example.employeemanagement.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new TreeMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return response(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DuplicateResourceException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Invalid value for '" + exception.getName() + "'",
                request.getRequestURI(),
                Map.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors) {
        ApiError error = new ApiError(
                status.value(), status.getReasonPhrase(), message, path, fieldErrors);
        return ResponseEntity.status(status).body(error);
    }
}

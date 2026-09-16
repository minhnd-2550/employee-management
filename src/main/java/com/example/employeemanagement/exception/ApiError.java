package com.example.employeemanagement.exception;

import java.util.Map;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}

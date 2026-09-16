package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank(message = "Department name must not be blank")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String name) {
}

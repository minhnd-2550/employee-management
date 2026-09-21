package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 50, message = "Username must contain 3 to 50 characters")
        String username,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 100, message = "Password must contain 8 to 100 characters")
        String password) {
}

package com.example.employeemanagement.dto;

import com.example.employeemanagement.model.Role;

public record AuthResponse(String token, String tokenType, String username, Role role) {
}

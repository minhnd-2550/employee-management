package com.example.employeemanagement.dto;

import com.example.employeemanagement.model.Role;

public record UserResponse(Long id, String username, Role role) {
}

package com.example.employeemanagement.dto;

public record UpdateEmployeeRequest(String name, String email, Long departmentId) {
}

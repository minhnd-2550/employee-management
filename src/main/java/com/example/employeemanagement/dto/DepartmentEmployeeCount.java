package com.example.employeemanagement.dto;

public record DepartmentEmployeeCount(
        Long departmentId,
        String departmentName,
        Long employeeCount) {
}

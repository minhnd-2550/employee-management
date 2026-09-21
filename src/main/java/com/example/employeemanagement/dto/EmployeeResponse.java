package com.example.employeemanagement.dto;

import com.example.employeemanagement.model.Employee;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String code,
        String name,
        String email,
        LocalDate hireDate,
        DepartmentResponse department) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getCode(),
                employee.getName(),
                employee.getEmail(),
                employee.getHireDate(),
                DepartmentResponse.from(employee.getDepartment()));
    }
}

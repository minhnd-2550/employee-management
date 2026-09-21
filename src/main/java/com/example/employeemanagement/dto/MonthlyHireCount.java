package com.example.employeemanagement.dto;

public record MonthlyHireCount(Integer year, Integer month, Long hiredCount) {

    public String monthLabel() {
        return "%02d/%d".formatted(month, year);
    }
}

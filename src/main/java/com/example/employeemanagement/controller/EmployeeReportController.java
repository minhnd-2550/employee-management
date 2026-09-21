package com.example.employeemanagement.controller;

import com.example.employeemanagement.service.EmployeeReportService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class EmployeeReportController {

    private final EmployeeReportService employeeReportService;

    public EmployeeReportController(EmployeeReportService employeeReportService) {
        this.employeeReportService = employeeReportService;
    }

    @GetMapping("/employees/count")
    public Map<String, Long> countEmployees() {
        return Map.of("totalEmployees", employeeReportService.countEmployees());
    }
}

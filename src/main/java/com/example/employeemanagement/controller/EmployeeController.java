package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.service.EmployeeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<Employee> findAll() {
        return employeeService.findAll();
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody CreateEmployeeRequest request) {
        Employee employee = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }
}

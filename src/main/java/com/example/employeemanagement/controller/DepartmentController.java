package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.CreateDepartmentRequest;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> findAll() {
        return departmentService.findAll();
    }

    @PostMapping
    public ResponseEntity<Department> create(
            @Valid @RequestBody CreateDepartmentRequest request) {
        Department department = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(department);
    }
}

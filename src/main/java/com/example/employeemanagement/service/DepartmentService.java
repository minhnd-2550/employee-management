package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.CreateDepartmentRequest;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.repository.DepartmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Transactional
    public Department create(CreateDepartmentRequest request) {
        return departmentRepository.save(new Department(request.name()));
    }
}

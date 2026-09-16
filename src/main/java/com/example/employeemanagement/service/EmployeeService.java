package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.model.Employee;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final List<Employee> employees = new ArrayList<>();
    private long nextId = 1;

    public synchronized List<Employee> findAll() {
        return List.copyOf(employees);
    }

    public synchronized Employee create(CreateEmployeeRequest request) {
        Employee employee = new Employee(nextId++, request.name(), request.email());
        employees.add(employee);
        return employee;
    }
}

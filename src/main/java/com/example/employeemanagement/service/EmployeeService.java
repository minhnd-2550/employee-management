package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.dto.UpdateEmployeeRequest;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> employeeNotFound(id));
    }

    public List<Employee> search(String name, Long departmentId) {
        boolean hasName = name != null && !name.isBlank();

        if (hasName && departmentId != null) {
            return employeeRepository.findByNameContainingIgnoreCaseAndDepartment_Id(
                    name, departmentId);
        }
        if (hasName) {
            return employeeRepository.findByNameContainingIgnoreCase(name);
        }
        if (departmentId != null) {
            return employeeRepository.findByDepartment_Id(departmentId);
        }
        return findAll();
    }

    @Transactional
    public Employee create(CreateEmployeeRequest request) {
        Department department = findDepartment(request.departmentId());
        return employeeRepository.save(
                new Employee(request.name(), request.email(), department));
    }

    @Transactional
    public Employee update(Long id, UpdateEmployeeRequest request) {
        Employee employee = findById(id);
        Department department = findDepartment(request.departmentId());
        employee.update(request.name(), request.email(), department);
        return employee;
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findById(id);
        employeeRepository.delete(employee);
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found: " + id));
    }

    private ResourceNotFoundException employeeNotFound(Long id) {
        return new ResourceNotFoundException("Employee not found: " + id);
    }
}

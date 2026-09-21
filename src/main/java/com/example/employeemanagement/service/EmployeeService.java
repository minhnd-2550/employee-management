package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.dto.UpdateEmployeeRequest;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UtilityService utilityService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            UtilityService utilityService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.utilityService = utilityService;
    }

    public Page<Employee> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> employeeNotFound(id));
    }

    public Page<Employee> search(String name, Long departmentId, Pageable pageable) {
        boolean hasName = name != null && !name.isBlank();

        if (hasName && departmentId != null) {
            return employeeRepository.findByNameContainingIgnoreCaseAndDepartment_Id(
                    name, departmentId, pageable);
        }
        if (hasName) {
            return employeeRepository.findByNameContainingIgnoreCase(name, pageable);
        }
        if (departmentId != null) {
            return employeeRepository.findByDepartment_Id(departmentId, pageable);
        }
        return findAll(pageable);
    }

    @Transactional
    @CacheEvict(cacheNames = "employeeCount", allEntries = true)
    public Employee create(CreateEmployeeRequest request) {
        Department department = findDepartment(request.departmentId());
        String name = utilityService.formatName(request.name());
        String email = request.email().strip();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        Employee employee = employeeRepository.save(
                new Employee(name, email, department, request.hireDate()));
        // The sequence comes from the generated id, so the code is unique per employee.
        employee.assignCode(utilityService.generateEmployeeCode(name, employee.getId()));

        log.info("Employee created: id={}, code={}, departmentId={}",
                employee.getId(), employee.getCode(), department.getId());
        return employee;
    }

    @Transactional
    public Employee update(Long id, UpdateEmployeeRequest request) {
        Employee employee = findById(id);
        Department department = findDepartment(request.departmentId());
        String email = request.email().strip();
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        employee.update(
                utilityService.formatName(request.name()),
                email,
                department,
                request.hireDate());
        log.info("Employee updated: id={}, departmentId={}", id, department.getId());
        return employee;
    }

    @Transactional
    @CacheEvict(cacheNames = "employeeCount", allEntries = true)
    public void delete(Long id) {
        Employee employee = findById(id);
        employeeRepository.delete(employee);
        log.info("Employee deleted: id={}", id);
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

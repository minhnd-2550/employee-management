package com.example.employeemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

@SpringBootTest
class EmployeeReportServiceTests {

    @Autowired
    private EmployeeReportService employeeReportService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearDataAndCache() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        Objects.requireNonNull(cacheManager.getCache("employeeCount")).clear();
    }

    @Test
    void cachesEmployeeCount() {
        Department engineering = departmentRepository.save(new Department("Engineering"));
        employeeRepository.save(new Employee("Nguyen Duc Minh", "minh@example.com", engineering));

        assertEquals(1, employeeReportService.countEmployees());

        employeeRepository.save(new Employee("Tran Thi Lan", "lan@example.com", engineering));
        assertEquals(1, employeeReportService.countEmployees());

        Objects.requireNonNull(cacheManager.getCache("employeeCount")).clear();
        assertEquals(2, employeeReportService.countEmployees());
    }
}

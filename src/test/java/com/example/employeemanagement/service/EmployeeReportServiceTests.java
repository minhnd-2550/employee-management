package com.example.employeemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.employeemanagement.dto.DepartmentEmployeeCount;
import com.example.employeemanagement.dto.MonthlyHireCount;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

@SpringBootTest
class EmployeeReportServiceTests {

    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 3, 15);

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
        employeeRepository.save(
                new Employee("Nguyen Duc Minh", "minh@example.com", engineering, HIRE_DATE));

        assertEquals(1, employeeReportService.countEmployees());

        employeeRepository.save(
                new Employee("Tran Thi Lan", "lan@example.com", engineering, HIRE_DATE));
        assertEquals(1, employeeReportService.countEmployees());

        Objects.requireNonNull(cacheManager.getCache("employeeCount")).clear();
        assertEquals(2, employeeReportService.countEmployees());
    }

    @Test
    void countsEmployeesForEveryDepartmentIncludingEmptyDepartments() {
        Department engineering = departmentRepository.save(new Department("Engineering"));
        Department operations = departmentRepository.save(new Department("Operations"));
        Department sales = departmentRepository.save(new Department("Sales"));

        employeeRepository.save(new Employee("Minh", "minh@example.com", engineering, HIRE_DATE));
        employeeRepository.save(new Employee("Lan", "lan@example.com", engineering, HIRE_DATE));
        employeeRepository.save(new Employee("An", "an@example.com", sales, HIRE_DATE));

        assertEquals(3, employeeReportService.countEmployees());
        assertEquals(
                List.of(
                        new DepartmentEmployeeCount(engineering.getId(), "Engineering", 2L),
                        new DepartmentEmployeeCount(operations.getId(), "Operations", 0L),
                        new DepartmentEmployeeCount(sales.getId(), "Sales", 1L)),
                employeeReportService.countEmployeesByDepartment());
    }

    @Test
    void countsHiresPerMonthInChronologicalOrder() {
        Department engineering = departmentRepository.save(new Department("Engineering"));

        employeeRepository.save(new Employee(
                "Minh", "minh@example.com", engineering, LocalDate.of(2024, 1, 5)));
        employeeRepository.save(new Employee(
                "Lan", "lan@example.com", engineering, LocalDate.of(2024, 3, 20)));
        employeeRepository.save(new Employee(
                "An", "an@example.com", engineering, LocalDate.of(2024, 3, 2)));
        employeeRepository.save(new Employee(
                "Binh", "binh@example.com", engineering, LocalDate.of(2023, 11, 9)));
        // Legacy rows without a hire date must not break the trend report.
        employeeRepository.save(new Employee(
                "Legacy", "legacy@example.com", engineering, null));

        List<MonthlyHireCount> trend = employeeReportService.countHiresByMonth();

        assertEquals(
                List.of(
                        new MonthlyHireCount(2023, 11, 1L),
                        new MonthlyHireCount(2024, 1, 1L),
                        new MonthlyHireCount(2024, 3, 2L)),
                trend);
        assertEquals("11/2023", trend.getFirst().monthLabel());
    }
}

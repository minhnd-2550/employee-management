package com.example.employeemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.dto.UpdateEmployeeRequest;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class EmployeeServiceTests {

    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 3, 15);
    private static final Pageable ALL = Pageable.unpaged(Sort.by("name"));

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void clearDatabase() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void performsCrudAndSearchesByNameOrDepartment(CapturedOutput output) {
        Department engineering = departmentRepository.save(new Department("Engineering"));
        Department sales = departmentRepository.save(new Department("Sales"));

        Employee minh = employeeService.create(new CreateEmployeeRequest(
                "Nguyen Duc Minh", "minh@example.com", engineering.getId(), HIRE_DATE));
        Employee lan = employeeService.create(new CreateEmployeeRequest(
                "Tran Thi Lan", "lan@example.com", sales.getId(), HIRE_DATE));

        assertEquals(2, employeeService.findAll(ALL).getTotalElements());
        assertEquals(minh.getId(), employeeService.findById(minh.getId()).getId());
        assertEquals(1, employeeService.search("minh", null, ALL).getTotalElements());
        assertEquals(lan.getId(),
                employeeService.search(null, sales.getId(), ALL).getContent().getFirst().getId());
        assertTrue(employeeService.search("minh", sales.getId(), ALL).isEmpty());

        Employee updated = employeeService.update(minh.getId(), new UpdateEmployeeRequest(
                "Nguyen Duc Minh Updated", "new-minh@example.com", sales.getId(),
                LocalDate.of(2024, 4, 1)));
        assertEquals("Nguyen Duc Minh Updated", updated.getName());
        assertEquals(sales.getId(), updated.getDepartment().getId());
        assertEquals(LocalDate.of(2024, 4, 1), updated.getHireDate());

        employeeService.delete(lan.getId());
        assertEquals(1, employeeService.findAll(ALL).getTotalElements());

        assertTrue(output.getAll().contains("Employee created: id=" + minh.getId()));
        assertTrue(output.getAll().contains("Employee updated: id=" + minh.getId()));
        assertTrue(output.getAll().contains("Employee deleted: id=" + lan.getId()));
    }

    @Test
    void assignsStableEmployeeCodeOnCreate() {
        Department engineering = departmentRepository.save(new Department("Engineering"));

        Employee minh = employeeService.create(new CreateEmployeeRequest(
                "  Nguyễn   Đức Minh ", "minh@example.com", engineering.getId(), HIRE_DATE));

        assertEquals("Nguyễn Đức Minh", minh.getName());
        assertEquals("NDM-%04d".formatted(minh.getId()), minh.getCode());

        Employee renamed = employeeService.update(minh.getId(), new UpdateEmployeeRequest(
                "Tran Thi Lan", "lan@example.com", engineering.getId(), HIRE_DATE));
        assertEquals("NDM-%04d".formatted(minh.getId()), renamed.getCode());
    }

    @Test
    void returnsRequestedPageSortedByName() {
        Department engineering = departmentRepository.save(new Department("Engineering"));
        for (String name : List.of("Cuong", "An", "Binh")) {
            employeeService.create(new CreateEmployeeRequest(
                    name, name.toLowerCase(Locale.ROOT) + "@example.com",
                    engineering.getId(), HIRE_DATE));
        }

        Page<Employee> firstPage = employeeService.findAll(
                PageRequest.of(0, 2, Sort.by("name")));
        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(List.of("An", "Binh"),
                firstPage.getContent().stream().map(Employee::getName).toList());

        Page<Employee> secondPage = employeeService.findAll(
                PageRequest.of(1, 2, Sort.by("name")));
        assertEquals(List.of("Cuong"),
                secondPage.getContent().stream().map(Employee::getName).toList());

        assertEquals(1, employeeService.search("in", null,
                PageRequest.of(0, 2, Sort.by("name"))).getTotalElements());
    }

    @Test
    void rejectsDuplicateEmailOnCreateAndUpdate() {
        Department engineering = departmentRepository.save(new Department("Engineering"));
        Employee minh = employeeService.create(new CreateEmployeeRequest(
                "Nguyen Duc Minh", "minh@example.com", engineering.getId(), HIRE_DATE));
        Employee lan = employeeService.create(new CreateEmployeeRequest(
                "Tran Thi Lan", "lan@example.com", engineering.getId(), HIRE_DATE));

        assertEquals("Email already exists: MINH@example.com",
                assertThrows(DuplicateResourceException.class,
                        () -> employeeService.create(new CreateEmployeeRequest(
                                "Someone Else", "MINH@example.com",
                                engineering.getId(), HIRE_DATE)))
                        .getMessage());

        assertThrows(DuplicateResourceException.class,
                () -> employeeService.update(lan.getId(), new UpdateEmployeeRequest(
                        "Tran Thi Lan", "minh@example.com", engineering.getId(), HIRE_DATE)));

        // Keeping the same email on the same employee stays allowed.
        assertEquals("minh@example.com",
                employeeService.update(minh.getId(), new UpdateEmployeeRequest(
                        "Nguyen Duc Minh", "minh@example.com",
                        engineering.getId(), HIRE_DATE)).getEmail());
    }
}

package com.example.employeemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.dto.UpdateEmployeeRequest;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
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
                "Nguyen Duc Minh", "minh@example.com", engineering.getId()));
        Employee lan = employeeService.create(new CreateEmployeeRequest(
                "Tran Thi Lan", "lan@example.com", sales.getId()));

        assertEquals(2, employeeService.findAll().size());
        assertEquals(minh.getId(), employeeService.findById(minh.getId()).getId());
        assertEquals(1, employeeService.search("minh", null).size());
        assertEquals(lan.getId(),
                employeeService.search(null, sales.getId()).getFirst().getId());
        assertTrue(employeeService.search("minh", sales.getId()).isEmpty());

        Employee updated = employeeService.update(minh.getId(), new UpdateEmployeeRequest(
                "Nguyen Duc Minh Updated", "new-minh@example.com", sales.getId()));
        assertEquals("Nguyen Duc Minh Updated", updated.getName());
        assertEquals(sales.getId(), updated.getDepartment().getId());

        employeeService.delete(lan.getId());
        assertEquals(1, employeeService.findAll().size());

        assertTrue(output.getAll().contains("Employee created: id=" + minh.getId()));
        assertTrue(output.getAll().contains("Employee updated: id=" + minh.getId()));
        assertTrue(output.getAll().contains("Employee deleted: id=" + lan.getId()));
    }
}

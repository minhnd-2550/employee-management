package com.example.employeemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.model.Employee;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmployeeServiceTests {

    @Test
    void createsEmployeesAndReturnsTheirSnapshot() {
        EmployeeService service = new EmployeeService();

        Employee first = service.create(
                new CreateEmployeeRequest("Nguyen Duc Minh", "minh@example.com"));
        Employee second = service.create(
                new CreateEmployeeRequest("Tran Thi Lan", "lan@example.com"));

        assertEquals(new Employee(1L, "Nguyen Duc Minh", "minh@example.com"), first);
        assertEquals(new Employee(2L, "Tran Thi Lan", "lan@example.com"), second);
        assertEquals(List.of(first, second), service.findAll());
    }
}

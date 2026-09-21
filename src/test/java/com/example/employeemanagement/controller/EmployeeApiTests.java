package com.example.employeemanagement.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.service.EmployeeService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class EmployeeApiTests {

    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 3, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void seedThreeEmployees() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        Department department = departmentRepository.save(new Department("Engineering"));
        for (String name : List.of("Cuong", "An", "Binh")) {
            employeeService.create(new CreateEmployeeRequest(
                    name, name.toLowerCase(Locale.ROOT) + "@example.com",
                    department.getId(), HIRE_DATE));
        }
    }

    @Test
    void returnsResponseDtoInsteadOfTheJpaEntity() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("An"))
                .andExpect(jsonPath("$.content[0].email").value("an@example.com"))
                .andExpect(jsonPath("$.content[0].hireDate").value("2024-03-15"))
                .andExpect(jsonPath("$.content[0].code")
                        .value(Matchers.matchesPattern("A-\\d{4}")))
                .andExpect(jsonPath("$.content[0].department.name").value("Engineering"))
                // The response exposes exactly six fields, so renaming an entity column
                // can no longer change the API contract by accident.
                .andExpect(jsonPath("$.content[0].length()").value(6))
                .andExpect(jsonPath("$.content[0].department.length()").value(2));
    }

    @Test
    void paginatesAndSortsTheEmployeeList() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("An"))
                .andExpect(jsonPath("$.content[1].name").value("Binh"))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/employees")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cuong"));

        mockMvc.perform(get("/api/employees/search")
                        .param("name", "in")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Binh"));
    }

    @Test
    void deniesRequestsThatNoRuleAllows() throws Exception {
        // PATCH has no mapping and no authorization rule, so it must not fall through
        // to "any authenticated user".
        mockMvc.perform(patch("/api/employees/1").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isForbidden());
    }
}

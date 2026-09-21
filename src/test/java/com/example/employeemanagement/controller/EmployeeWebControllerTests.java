package com.example.employeemanagement.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.service.EmployeeService;
import java.time.LocalDate;
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
class EmployeeWebControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void clearDatabase() {
        // Rolled back with the test transaction; keeps the counts independent of
        // whatever another test class committed earlier in the run.
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void rendersListFormAndSearchAndAcceptsValidSubmission() throws Exception {
        Department department = departmentRepository.save(new Department("Lab 6 Engineering"));

        mockMvc.perform(get("/employees/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(content().string(Matchers.containsString("Lab 6 Engineering")));

        mockMvc.perform(get("/employees/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/add"))
                .andExpect(content().string(Matchers.containsString("Thêm nhân viên")));

        mockMvc.perform(post("/employees/add").with(csrf())
                        .param("name", " ")
                        .param("email", "invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/add"))
                .andExpect(model().attributeHasFieldErrors(
                        "employeeForm", "name", "email", "departmentId", "hireDate"));

        mockMvc.perform(post("/employees/add").with(csrf())
                        .param("name", "Lab Six Tester")
                        .param("email", "lab6@example.com")
                        .param("departmentId", department.getId().toString())
                        .param("hireDate", "2024-03-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees/list"));

        mockMvc.perform(get("/employees/search")
                        .param("name", "six")
                        .param("departmentId", department.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(content().string(Matchers.containsString("Lab Six Tester")))
                .andExpect(content().string(Matchers.containsString("LST-")));

        mockMvc.perform(get("/employees/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/statistics"))
                .andExpect(model().attribute("totalEmployees", 1L))
                .andExpect(content().string(Matchers.containsString("Lab 6 Engineering")))
                .andExpect(content().string(Matchers.containsString("Tổng số nhân viên")))
                .andExpect(content().string(Matchers.containsString("03/2024")));
    }

    @Test
    void editsAndDeletesEmployeeFromTheWebPages() throws Exception {
        Department department = departmentRepository.save(new Department("Lab 6 Operations"));
        Employee employee = employeeService.create(new CreateEmployeeRequest(
                "Web Edit Target", "web-edit@example.com", department.getId(),
                LocalDate.of(2024, 3, 15)));
        String editUrl = "/employees/" + employee.getId() + "/edit";

        mockMvc.perform(get(editUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/edit"))
                .andExpect(model().attribute("employee", Matchers.notNullValue()))
                .andExpect(content().string(Matchers.containsString("web-edit@example.com")));

        mockMvc.perform(post(editUrl).with(csrf())
                        .param("name", " ")
                        .param("email", "invalid")
                        .param("departmentId", department.getId().toString())
                        .param("hireDate", "2024-03-15"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/edit"))
                .andExpect(model().attributeHasFieldErrors("employeeForm", "name", "email"));

        mockMvc.perform(post(editUrl).with(csrf())
                        .param("name", "Web Edit Renamed")
                        .param("email", "web-renamed@example.com")
                        .param("departmentId", department.getId().toString())
                        .param("hireDate", "2024-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees/list"));

        mockMvc.perform(get("/employees/list"))
                .andExpect(content().string(Matchers.containsString("Web Edit Renamed")))
                .andExpect(content().string(Matchers.containsString("2024-05-01")));

        mockMvc.perform(post("/employees/" + employee.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees/list"));

        mockMvc.perform(get("/employees/list"))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("Web Edit Renamed"))));
    }

    @Test
    @WithMockUser(roles = "USER")
    void keepsEditAndDeletePagesAdminOnly() throws Exception {
        Department department = departmentRepository.save(new Department("Lab 9 Operations"));
        Employee employee = employeeService.create(new CreateEmployeeRequest(
                "Read Only Target", "read-only@example.com", department.getId(),
                LocalDate.of(2024, 3, 15)));

        mockMvc.perform(get("/employees/" + employee.getId() + "/edit"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/employees/" + employee.getId() + "/delete").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/employees/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("Hành động"))));
    }
}

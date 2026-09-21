package com.example.employeemanagement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.repository.DepartmentRepository;
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

    @Test
    void rendersListFormAndSearchAndAcceptsValidSubmission() throws Exception {
        Department department = departmentRepository.save(new Department("Lab 6 Engineering"));

        mockMvc.perform(get("/employees/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Lab 6 Engineering")));

        mockMvc.perform(get("/employees/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/add"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Thêm nhân viên")));

        mockMvc.perform(post("/employees/add").with(csrf())
                        .param("name", " ")
                        .param("email", "invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/add"))
                .andExpect(model().attributeHasFieldErrors(
                        "employeeForm", "name", "email", "departmentId"));

        mockMvc.perform(post("/employees/add").with(csrf())
                        .param("name", "Lab Six Tester")
                        .param("email", "lab6@example.com")
                        .param("departmentId", department.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees/list"));

        mockMvc.perform(get("/employees/search")
                        .param("name", "six")
                        .param("departmentId", department.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Lab Six Tester")));

        mockMvc.perform(get("/employees/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees/statistics"))
                .andExpect(model().attribute("totalEmployees", 1L))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Lab 6 Engineering")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Tổng số nhân viên")));
    }
}

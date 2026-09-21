package com.example.employeemanagement.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.employeemanagement.model.AppUser;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.repository.AppUserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void registersUserHashesPasswordAndRejectsDuplicateUsername() throws Exception {
        String requestBody = """
                {"username":"Lab9User","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("lab9user"))
                .andExpect(jsonPath("$.role").value("USER"));

        AppUser savedUser = userRepository.findByUsername("lab9user").orElseThrow();
        assertThat(savedUser.getPassword()).startsWith("{bcrypt}");
        assertThat(savedUser.getPassword()).doesNotContain("password123");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: lab9user"));
    }

    @Test
    void authenticatesWithBasicAndJwtAndKeepsUserReadOnly() throws Exception {
        registerUser();

        mockMvc.perform(get("/api/employees")
                        .with(httpBasic("lab9user", "password123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/employees")
                        .with(httpBasic("lab9user", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Blocked","email":"blocked@example.com","departmentId":1}
                                """))
                .andExpect(status().isForbidden());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lab9user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousAccessAndInvalidLogin() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"unknown","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private void registerUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lab9user","password":"password123"}
                                """))
                .andExpect(status().isCreated());
    }
}

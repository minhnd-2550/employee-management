package com.example.employeemanagement.config;

import com.example.employeemanagement.model.AppUser;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod"})
public class AdminInitializer implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin.username}") String username,
            @Value("${app.security.admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username.trim().toLowerCase(java.util.Locale.ROOT);
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new AppUser(
                    username,
                    passwordEncoder.encode(password),
                    Role.ADMIN));
        }
    }
}

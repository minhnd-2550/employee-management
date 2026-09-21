package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.AuthResponse;
import com.example.employeemanagement.dto.LoginRequest;
import com.example.employeemanagement.dto.RegisterRequest;
import com.example.employeemanagement.dto.UserResponse;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.exception.InvalidCredentialsException;
import com.example.employeemanagement.model.AppUser;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.repository.AppUserRepository;
import com.example.employeemanagement.security.JwtService;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = normalize(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }

        AppUser user = userRepository.save(new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                Role.USER));
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        String username = normalize(request.username());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        return new AuthResponse(
                jwtService.createToken(user), "Bearer", user.getUsername(), user.getRole());
    }

    private String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}

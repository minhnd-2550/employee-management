package com.example.employeemanagement.config;

import com.example.employeemanagement.security.DatabaseUserDetailsService;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String ISSUER = "employee-management";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/css/**", "/error")
                        .permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/metrics/**").hasRole("ADMIN")
                        .requestMatchers("/employees/add").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/employees", "/api/departments")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/employees/list",
                                "/employees/search",
                                "/api/employees/**",
                                "/api/departments/**",
                                "/api/reports/**")
                        .hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.security.jwt.secret}") String encodedSecret) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedSecret);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
            return authorities == null ? Collections.emptyList() : authorities;
        });
        return converter;
    }
}

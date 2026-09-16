package com.example.employeemanagement.service;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class UtilityService {

    private final Clock clock;

    public UtilityService(Clock clock) {
        this.clock = clock;
    }

    public String formatName(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }

    public LocalDate currentDate() {
        return LocalDate.now(clock);
    }
}

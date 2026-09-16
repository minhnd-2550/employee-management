package com.example.employeemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class UtilityServiceTests {

    @Test
    void formatsNameAndUsesInjectedClockWithVietnamTimeZone() {
        // 18:00 UTC on December 31 is 01:00 on January 1 in Vietnam.
        Clock fixedClock = Clock.fixed(
                Instant.parse("2020-12-31T18:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        UtilityService service = new UtilityService(fixedClock);

        assertEquals("Nguyen Duc Minh", service.formatName("  Nguyen\t Duc\n Minh  "));
        assertEquals("", service.formatName("   "));
        assertEquals(LocalDate.of(2021, 1, 1), service.currentDate());
    }
}

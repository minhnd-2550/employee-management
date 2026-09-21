package com.example.employeemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class UtilityServiceTests {

    private final UtilityService service = new UtilityService(Clock.systemUTC());

    @Test
    void formatsNameAndUsesInjectedClockWithVietnamTimeZone() {
        // 18:00 UTC on December 31 is 01:00 on January 1 in Vietnam.
        Clock fixedClock = Clock.fixed(
                Instant.parse("2020-12-31T18:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        UtilityService serviceWithFixedClock = new UtilityService(fixedClock);

        assertEquals("Nguyen Duc Minh",
                serviceWithFixedClock.formatName("  Nguyen\t Duc\n Minh  "));
        assertEquals("", serviceWithFixedClock.formatName("   "));
        assertEquals(LocalDate.of(2021, 1, 1), serviceWithFixedClock.currentDate());
    }

    @Test
    void generatesEmployeeCodeFromInitialsAndSequence() {
        assertEquals("NDM-0001", service.generateEmployeeCode("Nguyen Duc Minh", 1));
        assertEquals("NDM-0042", service.generateEmployeeCode("  Nguyễn   Đức Minh ", 42));
        assertEquals("TTL-1234", service.generateEmployeeCode("Trần Thị Lan", 1234));
    }

    @Test
    void keepsAtMostThreeInitialsAndFallsBackWhenNameHasNoLetters() {
        assertEquals("BCD-0005",
                service.generateEmployeeCode("An Bui Cao Duong", 5));
        assertEquals("EMP-0007", service.generateEmployeeCode("  ??? ", 7));
    }
}

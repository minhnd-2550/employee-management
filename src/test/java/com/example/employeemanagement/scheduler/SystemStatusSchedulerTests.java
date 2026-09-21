package com.example.employeemanagement.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class SystemStatusSchedulerTests {

    @Test
    void logsSystemRunning(CapturedOutput output) {
        new SystemStatusScheduler().logSystemStatus();

        assertTrue(output.getAll().contains("System running"));
    }
}

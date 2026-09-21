package com.example.employeemanagement.scheduler;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SystemStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(SystemStatusScheduler.class);

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    public void logSystemStatus() {
        log.info("System running");
    }
}

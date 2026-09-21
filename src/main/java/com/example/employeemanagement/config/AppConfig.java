package com.example.employeemanagement.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
public class AppConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}

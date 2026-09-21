package com.example.employeemanagement.service;

import com.example.employeemanagement.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeReportService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeReportService.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeReportService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Cacheable("employeeCount")
    public long countEmployees() {
        log.debug("Counting employees in database");
        return employeeRepository.count();
    }
}

package com.example.employeemanagement.controller;

import com.example.employeemanagement.service.UtilityService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeePreviewController {

    private final UtilityService utilityService;

    public EmployeePreviewController(UtilityService utilityService) {
        this.utilityService = utilityService;
    }

    @GetMapping("/employees/preview")
    public Map<String, String> preview(
            @RequestParam(defaultValue = "  Nguyen   Duc Minh  ") String name) {
        return Map.of(
                "originalName", name,
                "formattedName", utilityService.formatName(name),
                "currentDate", utilityService.currentDate().toString());
    }
}

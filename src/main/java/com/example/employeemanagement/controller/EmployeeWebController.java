package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.dto.EmployeeForm;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.service.DepartmentService;
import com.example.employeemanagement.service.EmployeeReportService;
import com.example.employeemanagement.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/employees")
public class EmployeeWebController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final EmployeeReportService employeeReportService;

    public EmployeeWebController(
            EmployeeService employeeService,
            DepartmentService departmentService,
            EmployeeReportService employeeReportService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.employeeReportService = employeeReportService;
    }

    @GetMapping("/list")
    public String list(Model model, HttpServletRequest request) {
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("canManageEmployees", request.isUserInRole("ADMIN"));
        return "employees/list";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            Model model,
            HttpServletRequest request) {
        model.addAttribute("employees", employeeService.search(name, departmentId));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("name", name);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("searched", true);
        model.addAttribute("canManageEmployees", request.isUserInRole("ADMIN"));
        return "employees/list";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("totalEmployees", employeeReportService.countEmployees());
        model.addAttribute(
                "departmentStatistics",
                employeeReportService.countEmployeesByDepartment());
        return "employees/statistics";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("employeeForm", new EmployeeForm());
        model.addAttribute("departments", departmentService.findAll());
        return "employees/add";
    }

    @PostMapping("/add")
    public String add(
            @Valid @ModelAttribute("employeeForm") EmployeeForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            return "employees/add";
        }

        try {
            employeeService.create(new CreateEmployeeRequest(
                    form.getName(), form.getEmail(), form.getDepartmentId()));
        } catch (ResourceNotFoundException exception) {
            bindingResult.rejectValue("departmentId", "notFound", "Department does not exist");
            model.addAttribute("departments", departmentService.findAll());
            return "employees/add";
        }

        return "redirect:/employees/list";
    }
}

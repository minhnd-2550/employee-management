package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.CreateEmployeeRequest;
import com.example.employeemanagement.dto.DepartmentResponse;
import com.example.employeemanagement.dto.EmployeeForm;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.dto.UpdateEmployeeRequest;
import com.example.employeemanagement.exception.DuplicateResourceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.service.DepartmentService;
import com.example.employeemanagement.service.EmployeeReportService;
import com.example.employeemanagement.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/employees")
public class EmployeeWebController {

    private static final String LIST_REDIRECT = "redirect:/employees/list";
    // The pages show every match at once; the REST API is the paginated entry point.
    private static final Pageable ALL_BY_NAME = Pageable.unpaged(Sort.by("name"));

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
        model.addAttribute("employees", findEmployees(null, null));
        model.addAttribute("departments", findDepartments());
        model.addAttribute("canManageEmployees", request.isUserInRole("ADMIN"));
        return "employees/list";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            Model model,
            HttpServletRequest request) {
        model.addAttribute("employees", findEmployees(name, departmentId));
        model.addAttribute("departments", findDepartments());
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
        model.addAttribute("hiringTrend", employeeReportService.countHiresByMonth());
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
            return backToForm("employees/add", model);
        }

        try {
            employeeService.create(new CreateEmployeeRequest(
                    form.getName(), form.getEmail(), form.getDepartmentId(), form.getHireDate()));
        } catch (ResourceNotFoundException | DuplicateResourceException exception) {
            rejectConflict(bindingResult, exception);
            return backToForm("employees/add", model);
        }

        return LIST_REDIRECT;
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        EmployeeResponse employee = EmployeeResponse.from(employeeService.findById(id));

        EmployeeForm form = new EmployeeForm();
        form.setName(employee.name());
        form.setEmail(employee.email());
        form.setDepartmentId(employee.department().id());
        form.setHireDate(employee.hireDate());

        model.addAttribute("employeeForm", form);
        model.addAttribute("employee", employee);
        return backToForm("employees/edit", model);
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("employeeForm") EmployeeForm form,
            BindingResult bindingResult,
            Model model) {
        model.addAttribute("employee", EmployeeResponse.from(employeeService.findById(id)));

        if (bindingResult.hasErrors()) {
            return backToForm("employees/edit", model);
        }

        try {
            employeeService.update(id, new UpdateEmployeeRequest(
                    form.getName(), form.getEmail(), form.getDepartmentId(), form.getHireDate()));
        } catch (ResourceNotFoundException | DuplicateResourceException exception) {
            rejectConflict(bindingResult, exception);
            return backToForm("employees/edit", model);
        }

        return LIST_REDIRECT;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        employeeService.delete(id);
        return LIST_REDIRECT;
    }

    private String backToForm(String view, Model model) {
        model.addAttribute("departments", findDepartments());
        return view;
    }

    private List<EmployeeResponse> findEmployees(String name, Long departmentId) {
        return employeeService.search(name, departmentId, ALL_BY_NAME)
                .map(EmployeeResponse::from)
                .getContent();
    }

    private List<DepartmentResponse> findDepartments() {
        return departmentService.findAll().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    private void rejectConflict(BindingResult bindingResult, RuntimeException exception) {
        if (exception instanceof DuplicateResourceException) {
            bindingResult.rejectValue("email", "duplicate", exception.getMessage());
        } else {
            bindingResult.rejectValue("departmentId", "notFound", "Department does not exist");
        }
    }
}

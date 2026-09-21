package com.example.employeemanagement.repository;

import com.example.employeemanagement.dto.DepartmentEmployeeCount;
import com.example.employeemanagement.dto.MonthlyHireCount;
import com.example.employeemanagement.model.Employee;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Employee> findByDepartment_Id(Long departmentId, Pageable pageable);

    Page<Employee> findByNameContainingIgnoreCaseAndDepartment_Id(
            String name, Long departmentId, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("select count(employee) from Employee employee")
    long countAllEmployees();

    @Query("""
            select new com.example.employeemanagement.dto.DepartmentEmployeeCount(
                department.id,
                department.name,
                count(employee.id)
            )
            from Department department
            left join Employee employee on employee.department = department
            group by department.id, department.name
            order by department.name
            """)
    List<DepartmentEmployeeCount> countEmployeesByDepartment();

    @Query("""
            select new com.example.employeemanagement.dto.MonthlyHireCount(
                year(employee.hireDate),
                month(employee.hireDate),
                count(employee.id)
            )
            from Employee employee
            where employee.hireDate is not null
            group by year(employee.hireDate), month(employee.hireDate)
            order by year(employee.hireDate), month(employee.hireDate)
            """)
    List<MonthlyHireCount> countHiresByMonth();
}

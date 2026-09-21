package com.example.employeemanagement.repository;

import com.example.employeemanagement.dto.DepartmentEmployeeCount;
import com.example.employeemanagement.model.Employee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByNameContainingIgnoreCase(String name);

    List<Employee> findByDepartment_Id(Long departmentId);

    List<Employee> findByNameContainingIgnoreCaseAndDepartment_Id(
            String name, Long departmentId);

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
}

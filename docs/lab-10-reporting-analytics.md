# Lab 10 — Reporting & Analytics

## Yêu cầu đã thực hiện

- Thống kê tổng số nhân viên trong hệ thống.
- Thống kê số nhân viên của từng phòng ban bằng JPQL `@Query`.
- Giữ các phòng ban chưa có nhân viên trong kết quả với số lượng `0`.
- Cung cấp kết quả qua REST API.
- Hiển thị báo cáo tại <http://localhost:8080/employees/statistics>.

## REST API

Các endpoint yêu cầu đăng nhập với vai trò `USER` hoặc `ADMIN`.

### Tổng số nhân viên

```http
GET /api/reports/employees/count
```

Ví dụ response:

```json
{
  "totalEmployees": 3
}
```

Kết quả này tiếp tục dùng cache một phút từ Lab 8. Cache được xóa khi thêm hoặc
xóa nhân viên.

### Số nhân viên theo phòng ban

```http
GET /api/reports/employees/by-department
```

Ví dụ response:

```json
[
  {
    "departmentId": 1,
    "departmentName": "Engineering",
    "employeeCount": 2
  },
  {
    "departmentId": 2,
    "departmentName": "Sales",
    "employeeCount": 1
  }
]
```

Thử bằng Basic Auth:

```sh
curl --user lab9user:password123 \
  http://localhost:8080/api/reports/employees/by-department
```

## Truy vấn `@Query`

`EmployeeRepository` có hai truy vấn thống kê:

```java
@Query("select count(employee) from Employee employee")
long countAllEmployees();
```

Đây là JPQL nên `Employee` là tên entity Java, không phải tên bảng SQL
`employee`.

Thống kê theo phòng ban dùng constructor expression:

```java
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
```

Các thành phần cần hiểu:

- `left join`: lấy tất cả phòng ban, kể cả phòng ban chưa có nhân viên.
- `count(employee.id)`: đếm nhân viên trong mỗi nhóm; với phòng ban rỗng kết quả
  là `0`.
- `group by`: gom các dòng theo ID và tên phòng ban trước khi đếm.
- `order by`: sắp xếp kết quả theo tên phòng ban.
- `select new ...`: đưa kết quả thẳng vào DTO, không trả `Object[]` khó đọc.

Không cần thêm `@OneToMany` vào `Department`. Điều kiện join dùng quan hệ sở hữu
đã có ở `Employee.department`.

## Luồng REST API

```text
GET /api/reports/employees/by-department
    → Spring Security kiểm tra USER/ADMIN
    → EmployeeReportController
    → EmployeeReportService
    → EmployeeRepository @Query
    → MySQL
    → List<DepartmentEmployeeCount>
    → JSON response
```

Controller không chứa câu truy vấn. Service là nơi cung cấp nghiệp vụ báo cáo,
còn Repository chịu trách nhiệm truy cập database.

## Luồng trang Thymeleaf

```text
GET /employees/statistics
    → Spring Security kiểm tra USER/ADMIN
    → EmployeeWebController.statistics()
    → EmployeeReportService gọi hai truy vấn
    → Model: totalEmployees + departmentStatistics
    → templates/employees/statistics.html
    → HTML trả về trình duyệt
```

Trang thống kê không gọi REST API từ JavaScript. Đây vẫn là server-side rendering:
controller lấy dữ liệu, đưa vào `Model`, Thymeleaf render HTML trên server.

## Kiểm thử

```sh
./mvnw test
```

`EmployeeReportServiceTests` xác nhận:

- Tổng số nhân viên đúng.
- Mỗi phòng ban có số lượng đúng.
- Phòng ban không có nhân viên vẫn xuất hiện với `employeeCount = 0`.

`EmployeeWebControllerTests` xác nhận `/employees/statistics` trả đúng view và
hiển thị dữ liệu thống kê.

## Tự thực hành

1. Thêm một phòng ban chưa có nhân viên và kiểm tra kết quả API có số lượng `0`.
2. Thêm nhân viên vào phòng ban đó rồi tải lại trang thống kê.
3. Viết thêm truy vấn thống kê phòng ban có nhiều nhân viên nhất.
4. Thêm một cột phần trăm nhân viên của mỗi phòng ban so với toàn hệ thống.

Nguồn đề bài: <https://sun-asterisk.wsm.vn/learn/vi/learning/3824/content/4395/attachment/9053/>

# Lab 4 — Spring Data JPA và MySQL

## Đối chiếu yêu cầu

| Yêu cầu | Phần thực hiện |
| --- | --- |
| Cấu hình MySQL/PostgreSQL | Kết nối MySQL qua `application.properties` |
| Entity và `JpaRepository` | `Employee`, `Department` và hai repository |
| Bảng `employee`, `department` | Hibernate tạo/cập nhật từ entity |
| Quan hệ employee–department | `Employee.department` dùng `@ManyToOne` và `department_id` |
| CRUD Employee với database | GET, POST, PUT, DELETE dưới `/api/employees` |
| Tìm theo tên/phòng ban | `GET /api/employees/search` |

## Database local

Project mặc định kết nối:

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/employee_management
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

Có thể thay cấu hình mà không sửa code:

```sh
DB_URL='jdbc:mysql://127.0.0.1:3306/employee_management' \
DB_USERNAME=root \
DB_PASSWORD='your-password' \
./mvnw spring-boot:run
```

Tạo database nếu máy chưa có:

```sql
CREATE DATABASE employee_management
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

`ddl-auto=update` tiện cho bài học vì Hibernate tạo bảng khi chạy. Nó không phải
hệ thống migration an toàn cho production; khi schema cần được quản lý lâu dài,
dùng Flyway/Liquibase và chuyển Hibernate sang `validate`.

Test dùng H2 ở chế độ tương thích MySQL, được cấu hình trong
`src/test/resources/application.properties`. Vì vậy test không xóa hoặc sửa dữ
liệu MySQL local.

## Entity và quan hệ

`Department` ánh xạ bảng `department`; `Employee` ánh xạ bảng `employee`.

```java
@ManyToOne(fetch = FetchType.EAGER, optional = false)
@JoinColumn(name = "department_id", nullable = false)
private Department department;
```

Nhiều nhân viên có thể thuộc cùng một phòng ban. `Employee` là phía sở hữu quan
hệ vì bảng `employee` giữ khóa ngoại `department_id`. Lab này chỉ cần ánh xạ một
chiều từ Employee sang Department, tránh collection hai chiều và vòng lặp JSON.

JPA entity cần constructor không tham số để Hibernate khởi tạo. Constructor đó
dùng `protected`; code nghiệp vụ tạo employee bằng constructor còn lại.

`GenerationType.IDENTITY` để MySQL sinh ID tự tăng. Không dùng `columnDefinition`
để đặt tên cột; tên khóa ngoại nằm trong `@JoinColumn(name = "department_id")`.

## Repository

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByNameContainingIgnoreCase(String name);
    List<Employee> findByDepartment_Id(Long departmentId);
}
```

Spring Data JPA tạo implementation khi ứng dụng khởi động. `JpaRepository` đã có
`findAll`, `findById`, `save`, `delete`; chỉ khai báo thêm truy vấn tìm kiếm.
Spring phân tích tên method và sinh câu truy vấn tương ứng.

## Service và transaction

`EmployeeService` nhận hai repository qua constructor. Lớp dùng
`@Transactional(readOnly = true)` cho thao tác đọc; create/update/delete ghi đè
bằng `@Transactional` để thay đổi dữ liệu trong một transaction.

Khi update, entity được đọc trong transaction rồi gọi `employee.update(...)`.
Hibernate phát hiện thay đổi và cập nhật database khi transaction hoàn tất;
không cần gọi `save()` lần nữa cho entity đang được quản lý.

Nếu employee hoặc department không tồn tại, service tạm thời trả HTTP 404 bằng
`ResponseStatusException`. Lab 5 sẽ thay phần này bằng validation và global
exception handling có cấu trúc rõ hơn.

## Thực hành API

Chạy ứng dụng:

```sh
./mvnw spring-boot:run
```

### 1. Tạo phòng ban

```sh
curl -i -X POST http://localhost:8080/api/departments \
  -H 'Content-Type: application/json' \
  -d '{"name":"Engineering"}'
```

Ghi lại `id` trả về, ví dụ `1`.

### 2. Tạo nhân viên

```sh
curl -i -X POST http://localhost:8080/api/employees \
  -H 'Content-Type: application/json' \
  -d '{"name":"Nguyen Duc Minh","email":"minh@example.com","departmentId":1}'
```

### 3. Đọc dữ liệu

```sh
curl http://localhost:8080/api/employees
curl http://localhost:8080/api/employees/1
curl http://localhost:8080/api/departments
```

### 4. Tìm kiếm

```sh
curl -G http://localhost:8080/api/employees/search \
  --data-urlencode 'name=minh'

curl -G http://localhost:8080/api/employees/search \
  --data-urlencode 'departmentId=1'

curl -G http://localhost:8080/api/employees/search \
  --data-urlencode 'name=minh' \
  --data-urlencode 'departmentId=1'
```

Không truyền tham số thì search trả toàn bộ danh sách.

### 5. Cập nhật và xóa

```sh
curl -i -X PUT http://localhost:8080/api/employees/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Nguyen Duc Minh Updated","email":"new-minh@example.com","departmentId":1}'

curl -i -X DELETE http://localhost:8080/api/employees/1
```

DELETE thành công trả HTTP `204 No Content`.

## Khác với Lab 3

| Lab 3 | Lab 4 |
| --- | --- |
| `ArrayList` và biến `nextId` | `JpaRepository` và MySQL |
| Dữ liệu mất khi restart | Dữ liệu còn trong database |
| ID tăng trong service | MySQL sinh ID |
| Chỉ GET danh sách và POST | CRUD, quan hệ và tìm kiếm |

## Kiểm tra

```sh
./mvnw test
```

Test tích hợp dùng H2 để kiểm tra create, read, update, delete và ba trường hợp
tìm kiếm. Sau đó cần chạy ứng dụng với MySQL thật và gọi API để xác nhận schema,
khóa ngoại và dữ liệu vẫn còn sau khi restart.

Tham khảo:

- [Spring: Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql/)
- [Spring Boot: SQL Databases](https://docs.spring.io/spring-boot/reference/data/sql.html)

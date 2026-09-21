# Employee Management — Spring Boot Labs 1–10

Đây là trạng thái hoàn chỉnh sau 10 bài lab của Mini Project. Ứng dụng quản lý
nhân viên bằng REST API và Thymeleaf, lưu dữ liệu bằng Spring Data JPA, phân quyền
`USER`/`ADMIN`, cung cấp Actuator, cache, scheduled task và báo cáo thống kê.

## Chạy project hiện tại

Yêu cầu: Java 21 và MySQL đang chạy. Tạo database cho môi trường `dev`:

```sql
CREATE DATABASE employee_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Mặc định project kết nối tới `jdbc:mysql://127.0.0.1:3306/employee_management`
với username `root` và password rỗng. Có thể thay bằng biến môi trường:

```sh
DB_URL='jdbc:mysql://127.0.0.1:3306/employee_management' \
DB_USERNAME='root' \
DB_PASSWORD='your-password' \
./mvnw spring-boot:run
```

Profile mặc định là `dev`. Khi khởi động lần đầu, ứng dụng tạo tài khoản quản trị
dùng cho môi trường học tập:

```text
username: admin
password: admin12345
```

Không dùng các giá trị mặc định này khi triển khai thật. Profile `prod` bắt buộc
cung cấp `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_USERNAME`
và `ADMIN_PASSWORD`.

## Các địa chỉ chính

| Chức năng | Đường dẫn | Quyền |
| --- | --- | --- |
| Kiểm tra ứng dụng | `GET /hello` | Công khai |
| Demo bean và DI | `GET /employees/preview` | Công khai |
| Danh sách và tìm kiếm | `GET /employees/list` | `USER`, `ADMIN` |
| Form thêm nhân viên | `GET/POST /employees/add` | `ADMIN` |
| Form sửa nhân viên | `GET/POST /employees/{id}/edit` | `ADMIN` |
| Xóa nhân viên | `POST /employees/{id}/delete` | `ADMIN` |
| Trang thống kê | `GET /employees/statistics` | `USER`, `ADMIN` |
| REST API nhân viên | `/api/employees/**` | `USER` đọc, `ADMIN` CRUD |
| REST API phòng ban | `/api/departments/**` | `USER` đọc, `ADMIN` tạo |
| REST API báo cáo | `/api/reports/**` | `USER`, `ADMIN` |
| Đăng ký và đăng nhập | `/api/auth/register`, `/api/auth/login` | Công khai |
| Actuator health | `GET /actuator/health` | Công khai |
| Actuator metrics | `/actuator/metrics/**` | `ADMIN` |

Mọi đường dẫn không nằm trong bảng trên đều bị chặn bằng `denyAll()`, nên một
endpoint mới phải được khai báo trong `SecurityConfig` trước khi dùng được.

Trình duyệt sẽ hiện hộp thoại Basic Authentication khi mở các trang được bảo vệ.
Đăng nhập bằng `admin` / `admin12345`, rồi mở
<http://localhost:8080/employees/list>.

Kiểm tra hai lab đầu mà không cần đăng nhập:

```sh
curl http://localhost:8080/hello
curl -G http://localhost:8080/employees/preview \
  --data-urlencode 'name=  Nguyen   Duc Minh  '
```

Ví dụ gọi API bằng tài khoản quản trị:

```sh
curl --user admin:admin12345 \
  http://localhost:8080/api/employees

curl --user admin:admin12345 \
  http://localhost:8080/api/reports/employees/by-department

curl --user admin:admin12345 \
  http://localhost:8080/api/reports/employees/hiring-trend
```

## REST API nhân viên

`GET /api/employees` và `GET /api/employees/search` trả về kết quả phân trang với
`page`, `size` và `sort`. Mặc định 20 bản ghi mỗi trang, sắp xếp theo `name`, tối
đa 100 bản ghi mỗi trang:

```sh
curl --user admin:admin12345 \
  'http://localhost:8080/api/employees?page=0&size=2&sort=name,asc'
```

```json
{
  "content": [
    {
      "id": 1,
      "code": "NDM-0001",
      "name": "Nguyen Duc Minh",
      "email": "minh@example.com",
      "hireDate": "2024-03-15",
      "department": { "id": 1, "name": "Engineering" }
    }
  ],
  "page": { "size": 2, "number": 0, "totalElements": 3, "totalPages": 2 }
}
```

API trả về `EmployeeResponse` và `DepartmentResponse` chứ không trả thẳng entity
JPA, nên đổi tên cột trong database không làm vỡ hợp đồng API. Tầng Thymeleaf
cũng nhận DTO, entity chỉ tồn tại trong service và repository.

Các trang HTML vẫn hiển thị toàn bộ kết quả trong một trang; phân trang chỉ áp
dụng cho REST API.

## Báo cáo thống kê

| Báo cáo | Endpoint | Ghi chú |
| --- | --- | --- |
| Tổng số nhân viên | `GET /api/reports/employees/count` | Cache 1 phút |
| Số nhân viên theo phòng ban | `GET /api/reports/employees/by-department` | Gồm cả phòng ban trống |
| Xu hướng tuyển dụng theo tháng | `GET /api/reports/employees/hiring-trend` | Nhóm theo `hire_date` |

Cả ba báo cáo đều hiển thị trên `/employees/statistics`.

## Mã nhân viên và ràng buộc dữ liệu

`UtilityService.generateEmployeeCode` sinh mã từ chữ cái đầu của tên (đã bỏ dấu
tiếng Việt) và id, ví dụ `Nguyễn Đức Minh` với id 1 thành `NDM-0001`. Mã được gán
một lần khi tạo và không đổi khi sửa tên.

Bảng `employee` yêu cầu `name` và `email` không null, `email` và `code` là duy
nhất. Cột `hire_date` cho phép null ở database để dữ liệu cũ không bị vỡ, nhưng
mọi request tạo hoặc sửa đều bắt buộc có ngày vào làm.

Nếu database `dev` đã có sẵn dữ liệu từ các lab trước, `ddl-auto: update` sẽ
không thêm được unique index cho `email` khi còn email trùng. Xóa bản ghi trùng
hoặc tạo lại database trước khi chạy:

```sql
DROP DATABASE employee_management;
CREATE DATABASE employee_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

## Nội dung từng lab

| Lab | Nội dung | Hướng dẫn |
| --- | --- | --- |
| 1 | Khởi tạo Spring Boot và `GET /hello` | Phần “Cơ chế Lab 1” bên dưới |
| 2 | Bean, IoC và Dependency Injection | [Lab 2](docs/lab-02-beans-and-ioc.md) |
| 3 | REST API in-memory | [Lab 3](docs/lab-03-rest-api.md) |
| 4 | Spring Data JPA, MySQL, CRUD và tìm kiếm | [Lab 4](docs/lab-04-jpa-mysql.md) |
| 5 | Validation và Global Exception Handling | [Lab 5](docs/lab-05-validation-errors.md) |
| 6 | Spring MVC và Thymeleaf | [Lab 6](docs/lab-06-mvc-thymeleaf.md) |
| 7 | Logging và Profiles | [Lab 7](docs/lab-07-logging-profiles.md) |
| 8 | Actuator, Caching và Scheduling | [Lab 8](docs/lab-08-actuator-caching-scheduling.md) |
| 9 | Basic Auth, JWT và phân quyền | [Lab 9](docs/lab-09-spring-security.md) |
| 10 | REST API và Thymeleaf reporting | [Lab 10](docs/lab-10-reporting-analytics.md) |

Các tài liệu Lab 2–8 giải thích trạng thái tại thời điểm hoàn thành lab đó. Sau
Lab 9, các endpoint chứa dữ liệu nhân viên cần đăng nhập như bảng quyền phía trên.

## Luồng chính của ứng dụng

```text
HTTP request
  → Spring Security
  → Controller hoặc MVC Controller
  → Service
  → Repository
  → MySQL
  → JSON hoặc Thymeleaf HTML
```

- Controller nhận request và chọn HTTP response hoặc view.
- Service chứa nghiệp vụ, transaction, logging và cache invalidation.
- Repository dùng Spring Data JPA để truy vấn database.
- DTO nhận và validate dữ liệu từ client; entity biểu diễn dữ liệu được lưu.
- Thymeleaf render HTML trên server, không cần JavaScript gọi lại REST API.

## Cơ chế Lab 1

1. JVM chạy `main()` trong `EmployeeManagementApplication`.
2. `SpringApplication.run(...)` tạo ApplicationContext và web server nhúng.
3. `@SpringBootApplication` bật auto-configuration và component scanning.
4. Spring tìm thấy `HelloController` trong package con.
5. `@GetMapping("/hello")` ánh xạ request tới method `hello()`.
6. `@RestController` ghi chuỗi trả về trực tiếp vào HTTP response body.

Kết quả mong đợi của `GET /hello`:

```text
Hello, Employee Management!
```

## Kiểm thử và đóng gói

```sh
./mvnw test
./mvnw package
```

Test dùng H2 ở chế độ tương thích MySQL nên không thay đổi dữ liệu trong MySQL
cục bộ. Để chạy file JAR sau khi đóng gói:

```sh
java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

Nguồn đề bài: <https://sun-asterisk.wsm.vn/learn/vi/learning/3824/content/4395/attachment/9053/>

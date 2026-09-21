# Lab 7 — Logging và Profiles

## Yêu cầu đã thực hiện

| Đề bài | Cách thực hiện |
| --- | --- |
| Log khi thêm nhân viên | `EmployeeService.create(...)` ghi ID nhân viên và phòng ban |
| Log khi sửa nhân viên | `EmployeeService.update(...)` ghi ID nhân viên và phòng ban mới |
| Log khi xóa nhân viên | `EmployeeService.delete(...)` ghi ID nhân viên |
| Tách cấu hình DB dev/prod | `application-dev.yml` và `application-prod.yml` |

## Vì sao đặt log trong service?

Nhân viên có thể được thêm từ REST API hoặc form Thymeleaf. Cả hai luồng đều gọi
`EmployeeService`, nên đặt log tại service giúp mỗi thao tác chỉ cần một câu log
và không bỏ sót nguồn gọi nào:

```text
REST API ───────┐
                ├─> EmployeeService ─> EmployeeRepository ─> MySQL
Thymeleaf form ─┘          └─> SLF4J ─> Logback ─> console
```

Spring Boot starter đã cung cấp SLF4J và Logback, vì vậy Lab 7 không cần thêm
dependency. Code gọi API của SLF4J; Logback là implementation mặc định thực sự
định dạng và xuất log. Placeholder `{}` chỉ tạo chuỗi khi level tương ứng được
bật và dễ đọc hơn nối chuỗi bằng `+`.

Log chỉ chứa ID để theo dõi thao tác, không ghi email hoặc toàn bộ request nhằm
tránh đưa dữ liệu cá nhân vào log:

```text
Employee created: id=12, departmentId=2
Employee updated: id=12, departmentId=3
Employee deleted: id=12
```

## Profile dev và prod

`application.properties` chứa cấu hình dùng chung và chọn `dev` làm profile mặc
định để lệnh chạy local vẫn ngắn gọn:

```properties
spring.application.name=employee-management
spring.profiles.default=dev
spring.jpa.open-in-view=false
```

`application-dev.yml` có giá trị mặc định phù hợp máy phát triển:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://127.0.0.1:3306/employee_management}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update
```

Chạy dev:

```sh
./mvnw spring-boot:run
```

`application-prod.yml` bắt buộc nhận thông tin database từ biến môi trường và
dùng `ddl-auto: validate`. Ứng dụng chỉ kiểm tra schema có khớp entity hay không,
không tự ý sửa bảng production:

```sh
DB_URL='jdbc:mysql://db-host:3306/employee_management' \
DB_USERNAME='app_user' \
DB_PASSWORD='secret' \
SPRING_PROFILES_ACTIVE=prod \
java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

Không lưu mật khẩu production trực tiếp trong Git. `${DB_PASSWORD}` yêu cầu giá
trị được truyền từ môi trường chạy ứng dụng.

## Thứ tự nạp cấu hình

Khi chạy profile `dev`, Spring Boot đọc cấu hình chung rồi bổ sung/ghi đè bằng
`application-dev.yml`. Với `prod`, file `application-prod.yml` được dùng thay
cho file dev. Test kích hoạt profile `test` trong `src/test/resources`, nên dùng
H2 và không đụng vào MySQL local.

## Kiểm tra

```sh
./mvnw test
./mvnw spring-boot:run
```

Sau khi ứng dụng chạy, gọi API tạo, sửa và xóa nhân viên rồi quan sát console.
Test `EmployeeServiceTests` cũng bắt output để bảo đảm cả ba log được ghi.

Nguồn đề bài: <https://sun-asterisk.wsm.vn/learn/vi/learning/3824/content/4395/attachment/9053/>

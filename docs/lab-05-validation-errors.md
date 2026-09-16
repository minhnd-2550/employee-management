# Lab 5 — Validation và Exception Handling

## Đối chiếu yêu cầu

| Yêu cầu | Phần thực hiện |
| --- | --- |
| Email hợp lệ, name không rỗng | Constraint đặt trên create/update request |
| Employee không tồn tại trả 404 | `ResourceNotFoundException` và global handler |
| Request sai định dạng có lỗi rõ ràng | Handler cho JSON hỏng và sai kiểu path/query |
| Global Exception Handling | `@RestControllerAdvice` dùng chung cho các controller |

## Bean Validation

Project thêm dependency `spring-boot-starter-validation`. Request tạo và cập nhật
nhân viên chứa constraint:

```java
public record CreateEmployeeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email String email,
        @NotNull Long departmentId) {
}
```

Controller đặt `@Valid` trước `@RequestBody`:

```java
public ResponseEntity<Employee> create(
        @Valid @RequestBody CreateEmployeeRequest request) {
```

Luồng khi nhận request:

```text
JSON → chuyển thành DTO → chạy validation
                           ├─ hợp lệ → controller → service → database
                           └─ sai → MethodArgumentNotValidException
                                      ↓
                              GlobalExceptionHandler → HTTP 400
```

Constraint nằm trên DTO vì đây là dữ liệu đi vào API. Entity vẫn có database
constraint như `nullable = false`; hai lớp bảo vệ phục vụ hai mục đích khác nhau:
API trả lỗi dễ hiểu, database giữ tính toàn vẹn cuối cùng.

## Cấu trúc response lỗi

Mọi lỗi do Lab 5 xử lý có cùng dạng `ApiError`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/employees",
  "fieldErrors": {
    "departmentId": "Department ID is required",
    "email": "Email must be valid",
    "name": "Name must not be blank"
  }
}
```

- `status`: HTTP status dạng số.
- `error`: tên chuẩn của HTTP status.
- `message`: nội dung tổng quát, an toàn để trả cho client.
- `path`: endpoint phát sinh lỗi.
- `fieldErrors`: lỗi theo từng trường; rỗng với lỗi không thuộc một field.

Không trả stack trace hoặc nguyên nhân nội bộ của Jackson/Hibernate cho client.

## Global exception handler

`@RestControllerAdvice` đăng ký một bean áp dụng cho mọi REST controller. Các
method `@ExceptionHandler` chuyển exception thành `ResponseEntity<ApiError>`:

| Exception | Status | Trường hợp |
| --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | DTO vi phạm constraint |
| `HttpMessageNotReadableException` | 400 | JSON hỏng hoặc sai kiểu dữ liệu |
| `MethodArgumentTypeMismatchException` | 400 | ID/path/query sai kiểu |
| `ResourceNotFoundException` | 404 | Employee/Department không tồn tại |

Service ném `ResourceNotFoundException` mà không biết chi tiết JSON response.
Handler chịu trách nhiệm chuyển lỗi nghiệp vụ đó thành HTTP response. Cách tách
này giữ controller và service tập trung vào luồng thành công.

## Thử validation

Tên rỗng, email sai và thiếu department:

```sh
curl -i -X POST http://localhost:8080/api/employees \
  -H 'Content-Type: application/json' \
  -d '{"name":" ","email":"invalid-email"}'
```

JSON sai cú pháp:

```sh
curl -i -X POST http://localhost:8080/api/employees \
  -H 'Content-Type: application/json' \
  -d '{"name":}'
```

Employee không tồn tại:

```sh
curl -i http://localhost:8080/api/employees/999999999
```

ID sai kiểu:

```sh
curl -i http://localhost:8080/api/employees/not-a-number
```

Update cũng chạy cùng validation:

```sh
curl -i -X PUT http://localhost:8080/api/employees/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"","email":"bad","departmentId":2}'
```

## Kiểm tra

```sh
./mvnw test
```

`EmployeeValidationTests` dùng MockMvc kiểm tra bốn đường lỗi mà không cần mở
cổng mạng. Test vẫn dùng H2 nên không tác động dữ liệu MySQL local.

Tham khảo:

- [Spring MVC Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)
- [Spring Controller Advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)

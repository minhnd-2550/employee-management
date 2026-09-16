# Lab 3 — REST API cơ bản

## Mục tiêu và đối chiếu yêu cầu

| Yêu cầu thực hành | Phần thực hiện | Kết quả |
| --- | --- | --- |
| API lấy danh sách nhân viên từ bộ nhớ | `GET /api/employees` | Đạt |
| API thêm nhân viên mới | `POST /api/employees` | Đạt |

Lab này cũng áp dụng `@RestController`, `@RequestMapping`, `@GetMapping`,
`@PostMapping`, `@RequestBody` và `ResponseEntity` trong phần lý thuyết Module 3.

## Cấu trúc mới

```text
src/main/java/com/example/employeemanagement/
├── controller/EmployeeController.java
├── dto/CreateEmployeeRequest.java
├── model/Employee.java
└── service/EmployeeService.java
```

Luồng xử lý:

```text
HTTP request
    ↓
EmployeeController
    ↓ constructor injection
EmployeeService
    ↓
List<Employee> trong bộ nhớ
```

## Chạy và gọi API

Tại thư mục chứa `pom.xml`:

```sh
./mvnw spring-boot:run
```

Lấy danh sách ban đầu:

```sh
curl -i http://localhost:8080/api/employees
```

Kết quả là HTTP `200` với một mảng rỗng:

```json
[]
```

Thêm nhân viên:

```sh
curl -i -X POST http://localhost:8080/api/employees \
  -H 'Content-Type: application/json' \
  -d '{"name":"Nguyen Duc Minh","email":"minh@example.com"}'
```

Kết quả là HTTP `201 Created`:

```json
{
  "id": 1,
  "name": "Nguyen Duc Minh",
  "email": "minh@example.com"
}
```

Gọi lại `GET /api/employees` sẽ thấy nhân viên vừa thêm:

```json
[
  {
    "id": 1,
    "name": "Nguyen Duc Minh",
    "email": "minh@example.com"
  }
]
```

## Giải thích code

### Model Employee

```java
public record Employee(Long id, String name, String email) {
}
```

`record` phù hợp với đối tượng chỉ mang dữ liệu: Java tự tạo constructor,
accessor `id()`, `name()`, `email()`, cùng `equals()`, `hashCode()` và `toString()`.
Spring chuyển record này thành JSON khi trả response.

### DTO nhận request

```java
public record CreateEmployeeRequest(String name, String email) {
}
```

Client không được gửi `id`; service chịu trách nhiệm tạo ID. Việc tách request
khỏi `Employee` làm rõ dữ liệu client được phép cung cấp.

`@RequestBody` yêu cầu Spring đọc JSON trong HTTP body và chuyển nó thành
`CreateEmployeeRequest`. Header `Content-Type: application/json` cho biết định
dạng của body.

### Dữ liệu in-memory trong service

```java
private final List<Employee> employees = new ArrayList<>();
private long nextId = 1;
```

Danh sách tồn tại trong singleton `EmployeeService`. Khi ứng dụng dừng hoặc khởi
động lại, dữ liệu mất và ID trở lại `1`. Đây là đúng phạm vi Lab 3; Lab 4 sẽ thay
nó bằng database và Spring Data JPA.

Hai method dùng `synchronized` vì web server có thể xử lý nhiều request đồng thời.
Nó bảo vệ `ArrayList` và thao tác tăng `nextId`. Đây là cách ngắn gọn phù hợp với
bộ nhớ dùng cho bài học; database ở Lab 4 sẽ xử lý đồng thời theo cách khác.

`findAll()` trả `List.copyOf(employees)` để client của service nhận một snapshot,
không thể sửa trực tiếp danh sách nội bộ.

### Controller và HTTP method

```java
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
```

`@RequestMapping` đặt đường dẫn chung. Hai method bên trong tạo thành:

- `@GetMapping` → `GET /api/employees`: đọc danh sách, không thay đổi dữ liệu.
- `@PostMapping` → `POST /api/employees`: tạo một nhân viên mới.

Controller nhận `EmployeeService` qua constructor injection. Nó xử lý HTTP và
giao nghiệp vụ lưu/tạo ID cho service.

Method POST trả `ResponseEntity<Employee>` để chọn rõ HTTP status `201 Created`.
Method GET có thể trả `List<Employee>` trực tiếp; khi thành công Spring mặc định
trả HTTP `200 OK`.

## Điều chưa làm trong Lab 3

- Chưa kiểm tra tên rỗng hoặc email sai định dạng: validation thuộc Lab 5.
- Chưa lưu database: thuộc Lab 4.
- Chưa có sửa, xóa, tìm theo ID hoặc tìm kiếm: các lab sau sẽ bổ sung.
- Chưa chuẩn hóa tên khi POST để giữ Lab 3 tập trung vào REST; có thể tái sử dụng
  `UtilityService` khi nghiệp vụ yêu cầu trong bước tiếp theo.

## Test và bài tự thực hành

```sh
./mvnw test
```

`EmployeeServiceTests` kiểm tra ID tăng từ `1`, dữ liệu được thêm theo đúng thứ
tự và `findAll()` trả đủ danh sách.

Bạn hãy thử:

1. Thêm hai nhân viên và dự đoán ID của từng người.
2. Gọi GET trước và sau POST để quan sát trạng thái trong bộ nhớ.
3. Khởi động lại ứng dụng và giải thích vì sao danh sách trở về rỗng.
4. Gửi JSON thiếu `email`; ghi nhận hành vi hiện tại để so sánh sau Lab 5.

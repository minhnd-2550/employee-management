# Lab 6 — Spring MVC và Thymeleaf

## Yêu cầu đã thực hiện

| Đề bài | Đường dẫn |
| --- | --- |
| Danh sách nhân viên từ database | `GET /employees/list` |
| Form thêm nhân viên | `GET /employees/add`, `POST /employees/add` |
| Tìm theo tên và phòng ban | `GET /employees/search?name=...&departmentId=...` |

Giao diện dùng `EmployeeWebController` đánh dấu `@Controller`. Các REST controller
cũ vẫn dùng `@RestController`. Cả hai gọi cùng `EmployeeService` và
`DepartmentService`, nên dữ liệu trên web và JSON API giống nhau.

## Chạy ứng dụng

```sh
./mvnw spring-boot:run
```

Mở <http://localhost:8080/employees/list> rồi chọn **Thêm nhân viên**.
Nếu database mới chưa có phòng ban, tạo một phòng ban trước:

```sh
curl -X POST http://localhost:8080/api/departments \
  -H 'Content-Type: application/json' \
  -d '{"name":"Engineering"}'
```

## Luồng MVC

```text
Trình duyệt → EmployeeWebController → EmployeeService → MySQL
                      ↓
                    Model → Thymeleaf template → HTML trả về trình duyệt
```

`@Controller` trả tên view, ví dụ `"employees/list"`. Spring tìm file
`src/main/resources/templates/employees/list.html`, kết hợp dữ liệu trong `Model`
và render HTML. Với `@RestController`, chuỗi trả về sẽ là nội dung response,
không phải tên template.

Trang danh sách dùng `th:each` lặp nhân viên và `th:text` để hiển thị dữ liệu.
`th:text` escape HTML của dữ liệu do người dùng nhập. Form dùng `th:object`
để chọn object `employeeForm`, rồi `th:field` để nối input với các field Java.

Khi gửi form, Spring bind các trường vào `EmployeeForm`. `@Valid` kiểm tra dữ liệu,
và `BindingResult` nhận lỗi ngay sau tham số form. Nếu có lỗi, controller trả lại
template `employees/add` và nạp lại danh sách phòng ban; `th:errors` hiện lỗi
ngay dưới input. Nếu hợp lệ, controller gọi `EmployeeService.create(...)` rồi
redirect về trang danh sách. Redirect tránh tạo nhân viên lần nữa khi người dùng
refresh sau khi gửi form.

Trang tìm kiếm gọi cùng `EmployeeService.search(...)` với REST API. Tên và phòng
ban có thể dùng riêng hoặc kết hợp; sau khi tìm, form giữ các giá trị đã chọn.

## Kiểm tra

```sh
./mvnw test
```

Test web kiểm tra template danh sách, tìm kiếm và form hợp lệ/không hợp lệ. Test
dùng H2 riêng, không sửa dữ liệu MySQL local. Sau đó mở giao diện thật và thử
thêm một nhân viên để xác nhận HTML và database cùng hoạt động.

Tham khảo: [Spring: Handling Form Submission](https://spring.io/guides/gs/handling-form-submission/)

# Lab 9 — Spring Security Basics

## Yêu cầu đã thực hiện

- Tạo `AppUser` gồm `username`, `password`, `role` và lưu vào bảng `app_user`.
- Đăng ký tài khoản `USER` qua `POST /api/auth/register`.
- Đăng nhập qua `POST /api/auth/login` để nhận JWT có thời hạn 1 giờ.
- Hỗ trợ Basic Authentication cho các client đơn giản.
- `USER` được xem danh sách, tìm kiếm và báo cáo nhân viên.
- `ADMIN` có toàn bộ quyền của `USER`, đồng thời được thêm, sửa, xóa nhân viên
  và tạo phòng ban.

Entity có tên `AppUser` thay vì `User` để tránh nhầm với lớp `User` của Spring
Security và tránh dùng tên bảng dễ trùng từ khóa SQL.

## Chạy project

Profile mặc định là `dev`. Lần chạy đầu, `AdminInitializer` tự tạo tài khoản quản
trị cục bộ nếu username đó chưa có:

```text
username: admin
password: admin12345
```

Đây chỉ là giá trị mặc định để học trên máy cá nhân. Có thể thay bằng biến môi
trường:

```sh
export ADMIN_USERNAME=myadmin
export ADMIN_PASSWORD='a-strong-password'
export JWT_SECRET='base64-encoded-secret-with-at-least-32-bytes'
./mvnw spring-boot:run
```

Với profile `prod`, ba biến `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `JWT_SECRET` là
bắt buộc; project không cung cấp giá trị mặc định.

## 1. Đăng ký tài khoản USER

```sh
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"minh","password":"password123"}'
```

Response thành công có HTTP `201`:

```json
{
  "id": 2,
  "username": "minh",
  "role": "USER"
}
```

Client không được gửi `role` khi đăng ký. Server luôn gán `USER`, nếu không người
dùng có thể tự đăng ký làm `ADMIN`.

Mật khẩu không được lưu nguyên văn. `PasswordEncoder` tạo chuỗi dạng
`{bcrypt}...`; khi đăng nhập, Spring mã hóa mật khẩu nhận được rồi so sánh theo
cơ chế của BCrypt. Hash là một chiều, không phải mã hóa để giải mã lại.

## 2. Basic Authentication

Basic Auth gửi username/password trong header của từng request:

```sh
curl -i --user minh:password123 http://localhost:8080/api/employees
```

Tài khoản `USER` gọi request ghi dữ liệu sẽ nhận HTTP `403 Forbidden`:

```sh
curl -i -X POST --user minh:password123 \
  -H 'Content-Type: application/json' \
  -d '{"name":"New Employee","email":"new@example.com","departmentId":1}' \
  http://localhost:8080/api/employees
```

Basic Auth chỉ mã hóa Base64 phần thông tin đăng nhập, vì vậy khi triển khai thật
phải dùng HTTPS.

## 3. Đăng nhập và dùng JWT

Đăng nhập bằng tài khoản vừa đăng ký:

```sh
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"minh","password":"password123"}'
```

Response trả `tokenType: Bearer`, username, role và token. Sao chép giá trị
`token` rồi gửi trong header:

```sh
curl -i http://localhost:8080/api/employees \
  -H 'Authorization: Bearer PASTE_TOKEN_HERE'
```

JWT của lab chứa `sub` là username, claim `roles`, thời điểm phát hành và hết hạn.
Server ký token bằng HMAC SHA-256 và kiểm tra chữ ký, issuer, thời hạn ở mỗi
request. JWT được ký chứ không được mã hóa; không đưa mật khẩu hoặc dữ liệu bí mật
vào claims.

## 4. ADMIN thực hiện CRUD

Ví dụ tạo nhân viên bằng Basic Auth của tài khoản quản trị:

```sh
curl -i -X POST --user admin:admin12345 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Lab 9 Employee","email":"lab9@example.com","departmentId":1}' \
  http://localhost:8080/api/employees
```

Các request `PUT /api/employees/{id}` và `DELETE /api/employees/{id}` cũng yêu
cầu vai trò `ADMIN`. Trên giao diện server-side, mở
<http://localhost:8080/employees/list>; trình duyệt hỏi Basic Auth. `ADMIN` nhìn
thấy nút **Thêm nhân viên**, còn `USER` không thấy nút và cũng bị server từ chối
nếu tự gõ URL `/employees/add`.

## Ma trận quyền

| Endpoint | Không đăng nhập | USER | ADMIN |
| --- | --- | --- | --- |
| `POST /api/auth/register`, `POST /api/auth/login` | Cho phép | Cho phép | Cho phép |
| `GET /api/employees/**`, `/api/departments/**`, `/api/reports/**` | 401 | Cho phép | Cho phép |
| `GET /employees/list`, `/employees/search` | 401 | Cho phép | Cho phép |
| `POST /api/employees`, `PUT/DELETE /api/employees/**` | 401 | 403 | Cho phép |
| `POST /api/departments`, `/employees/add` | 401 | 403 | Cho phép |
| `/actuator/health` | Cho phép | Cho phép | Cho phép |
| `/actuator/metrics/**` | 401 | 403 | Cho phép |

`401 Unauthorized` nghĩa là chưa xác thực hoặc thông tin đăng nhập sai.
`403 Forbidden` nghĩa là đã xác thực nhưng không đủ quyền.

## Luồng xử lý cần hiểu

### Đăng nhập bằng username/password

1. `AuthController` nhận `LoginRequest`.
2. `AuthenticationManager` chuyển việc xác thực cho `DaoAuthenticationProvider`.
3. `DatabaseUserDetailsService` tải `AppUser` từ database.
4. `PasswordEncoder` kiểm tra mật khẩu hash.
5. `JwtService` tạo JWT sau khi xác thực thành công.

### Gọi API bằng Bearer token

1. Spring Security đọc header `Authorization: Bearer ...` trước controller.
2. `JwtDecoder` kiểm tra chữ ký HS256, issuer và thời gian hết hạn.
3. `JwtAuthenticationConverter` đổi claim `roles: ["USER"]` thành authority
   `ROLE_USER`.
4. `SecurityFilterChain` kiểm tra rule của URL và HTTP method.
5. Controller chỉ chạy nếu request đủ quyền.

Vì kiểm tra quyền nằm trong security filter, việc ẩn nút trên HTML chỉ cải thiện
trải nghiệm. Kẻ gọi API trực tiếp vẫn bị chặn tại server.

## Vì sao vẫn giữ Basic Auth và JWT?

- Basic Auth dễ thử bằng `curl` và phù hợp để hiểu cơ chế
  `UserDetailsService` + `PasswordEncoder`.
- JWT phù hợp cho frontend/mobile gọi REST API: client đăng nhập một lần rồi gửi
  token ở các request sau.
- Hai cách cùng đi qua các rule phân quyền, nên `USER` và `ADMIN` nhận cùng kết
  quả dù xác thực bằng Basic hay Bearer token.

## Kiểm thử

```sh
./mvnw test
```

`SecurityAuthorizationTests` kiểm tra đăng ký, username trùng, mật khẩu đã hash,
đăng nhập Basic/JWT, anonymous nhận 401 và `USER` ghi dữ liệu nhận 403. Các test
MVC cũ chạy với mock `ADMIN` và gửi CSRF token cho form POST.

## Tài liệu tham khảo

- [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/dao-authentication-provider.html)
- [Password Storage](https://docs.spring.io/spring-security/reference/7.0/features/authentication/password-storage.html)
- [Authorize HTTP Requests](https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/authorize-http-requests.html)
- [OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/resource-server/index.html)

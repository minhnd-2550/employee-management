# Employee Management — Labs 1–2

Hướng dẫn bên dưới dành cho Lab 1. Xem [Lab 2 — Bean, IoC và Dependency Injection](docs/lab-02-beans-and-ioc.md)
để học phần mới và thử API <http://localhost:8080/employees/preview>.

## Mục tiêu

Khởi tạo một ứng dụng Spring Boot và gọi thành công API `GET /hello`.
Lab này chưa cần database, service, đăng nhập hoặc giao diện.

Project được tạo bằng Spring Initializr: Maven, Java 21, Spring Boot 4.1.1,
dependency Spring Web. Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`) cho phép chạy
Maven mà không cần cài riêng. Lần đầu cần Internet để tải Maven và dependencies.

## Chạy ứng dụng

Mở terminal tại thư mục chứa file `pom.xml` rồi chạy:

```sh
./mvnw spring-boot:run
```

Mở <http://localhost:8080/hello> hoặc dùng terminal khác:

```sh
curl -i http://localhost:8080/hello
```

Kết quả mong đợi: HTTP `200` và nội dung:

```text
Hello, Employee Management!
```

Dừng bằng `Ctrl+C` trong terminal chạy ứng dụng. Nếu cổng 8080 đang được dùng:

```sh
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Khi đó dùng <http://localhost:8081/hello>. Đường dẫn `/` chưa được khai báo nên
trả 404 là bình thường; hãy truy cập đúng `/hello`.

## Cấu trúc cần hiểu

```text
employee-management/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/
└── src/
    ├── main/
    │   ├── java/com/example/employeemanagement/
    │   │   ├── EmployeeManagementApplication.java
    │   │   └── controller/HelloController.java
    │   └── resources/application.properties
    └── test/java/com/example/employeemanagement/
        └── EmployeeManagementApplicationTests.java
```

- `pom.xml`: phiên bản Java, Spring Boot, thư viện và cấu hình build.
- `EmployeeManagementApplication`: điểm bắt đầu của chương trình.
- `HelloController`: nhận request và trả response.
- `application.properties`: cấu hình ứng dụng; hiện có tên ứng dụng.
- Test được Initializr tạo sẵn kiểm tra Spring ApplicationContext khởi động được.

## Cơ chế hoạt động

1. JVM chạy `main()` trong `EmployeeManagementApplication`.
2. `SpringApplication.run(...)` tạo Spring ApplicationContext và khởi động web server nhúng.
3. `@SpringBootApplication` kết hợp cấu hình, auto-configuration và component scanning.
   Spring tìm các component trong package của lớp này và các package con,
   vì vậy đặt `controller` bên dưới `com.example.employeemanagement`.
4. `@RestController` khiến Spring quản lý `HelloController` và ghi giá trị trả về
   của method vào HTTP response body.
5. `@GetMapping("/hello")` ánh xạ request `GET /hello` tới method `hello()`.
6. Method trả chuỗi `Hello, Employee Management!`, trình duyệt hiển thị chuỗi đó.

Không cần tự viết `new HelloController()`. Chuỗi trả về là văn bản, chưa phải JSON
và cũng không phải tên một trang HTML.

**Starter** gom các dependency phục vụ một chức năng. Trong project Boot 4 này,
`spring-boot-starter-webmvc` cung cấp nền tảng Spring MVC và web server nhúng.
**Auto-configuration** dựa vào dependency và cấu hình hiện có để thiết lập các
thành phần phù hợp; nhờ đó chưa cần tự cấu hình web server cho Lab 1.

## Tự thực hành

1. Chạy project và gọi `/hello`.
2. Đổi nội dung trả về thành lời chào của bạn, khởi động lại và kiểm tra.
3. Thêm một method với `@GetMapping("/welcome")` rồi gọi endpoint mới.
4. Giải thích tại sao `/hello` chạy được nhưng `/` trả 404.

## Kiểm tra và đóng gói

```sh
./mvnw test
./mvnw package
java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

Chỉ chạy một phiên ứng dụng trên cùng cổng. Kiểm tra context không thay thế
việc gọi `/hello`: cần xác nhận HTTP 200 và đúng nội dung bằng curl hoặc trình duyệt.

Nguồn đề bài: <https://sun-asterisk.wsm.vn/learn/vi/learning/3824/content/4395/attachment/9053/>

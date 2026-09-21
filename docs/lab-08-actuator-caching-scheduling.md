# Lab 8 — Actuator, Caching và Scheduling

## Yêu cầu đã thực hiện

| Đề bài | Kết quả |
| --- | --- |
| Actuator health | `GET /actuator/health` |
| Actuator metrics | `GET /actuator/metrics` |
| API tổng số nhân viên | `GET /api/reports/employees/count` |
| Cache báo cáo 1 phút | Caffeine `expireAfterWrite=60s` |
| Log mỗi 30 giây | `SystemStatusScheduler` ghi `System running` |

## API báo cáo và cache

Luồng gọi API:

```text
GET /api/reports/employees/count
  → EmployeeReportController
  → EmployeeReportService.countEmployees()
  → cache employeeCount
       ├─ có dữ liệu: trả ngay
       └─ chưa có: EmployeeRepository.countAllEmployees() → MySQL → lưu cache 60 giây
```

`@Cacheable("employeeCount")` được đặt tại service. Spring tạo proxy bao quanh
method: lần gọi đầu chạy `employeeRepository.count()`, những lần tiếp theo dùng
kết quả đã cache. Caffeine xóa entry sau 60 giây tính từ lúc ghi:

```properties
spring.cache.cache-names=employeeCount
spring.cache.caffeine.spec=maximumSize=100,expireAfterWrite=60s,recordStats
```

`recordStats` cho phép Actuator thu thập cache hit, miss và eviction metrics.

Khi thêm hoặc xóa nhân viên, `@CacheEvict` xóa báo cáo cũ ngay lập tức. Việc sửa
nhân viên không làm thay đổi tổng số nên không cần xóa cache.

Response mẫu:

```json
{"totalEmployees":1}
```

## Scheduled task

`@EnableScheduling` bật cơ chế tìm các bean có method `@Scheduled`.
`SystemStatusScheduler` chạy theo fixed rate 30 giây:

```java
@Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
public void logSystemStatus() {
    log.info("System running");
}
```

`fixedRate` đo khoảng cách giữa thời điểm bắt đầu hai lần chạy. Nếu công việc cần
đợi lần trước hoàn thành rồi mới tính 30 giây thì dùng `fixedDelay`.

## Actuator

Dependency `spring-boot-starter-actuator` cung cấp các endpoint vận hành. Project
chỉ expose hai endpoint cần cho Lab 8:

```properties
management.endpoints.web.exposure.include=health,metrics
```

- `/actuator/health` cho biết ứng dụng và database có hoạt động không.
- `/actuator/metrics` liệt kê metric JVM, HTTP, datasource và nhiều thành phần khác.
- `/actuator/metrics/jvm.memory.used` xem chi tiết một metric cụ thể.

Không expose toàn bộ endpoint bằng `*` vì các endpoint như `env` hoặc `beans` có
thể làm lộ thông tin nội bộ. Khi ứng dụng public, các endpoint quản trị cần được
bảo vệ bằng Spring Security hoặc chỉ mở trong mạng nội bộ.

## Chạy và kiểm tra

```sh
./mvnw test
./mvnw spring-boot:run

curl --user admin:admin12345 http://localhost:8080/api/reports/employees/count
curl http://localhost:8080/actuator/health
curl --user admin:admin12345 http://localhost:8080/actuator/metrics
```

Sau khi hoàn thành Lab 9, API báo cáo yêu cầu `USER` hoặc `ADMIN`, còn Actuator
metrics yêu cầu `ADMIN`. Endpoint health vẫn được mở để hệ thống giám sát gọi.

Test báo cáo xác nhận lần gọi thứ hai lấy số lượng cũ từ cache; sau khi xóa cache,
service đọc lại giá trị mới từ database. Test scheduler gọi method trực tiếp và
kiểm tra nội dung log, không phải chờ đủ 30 giây trong test suite.

Nguồn đề bài: <https://sun-asterisk.wsm.vn/learn/vi/learning/3824/content/4395/attachment/9053/>

Tài liệu chính thức:

- <https://docs.spring.io/spring-boot/reference/io/caching.html>
- <https://docs.spring.io/spring-boot/reference/actuator/monitoring.html>
- <https://docs.spring.io/spring-boot/reference/actuator/metrics.html>
- <https://docs.spring.io/spring-framework/reference/integration/scheduling.html>

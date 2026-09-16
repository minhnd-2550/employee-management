# Lab 2 — Custom Bean, IoC và Dependency Injection

## Mục tiêu và phần đã làm

Theo đề bài, Lab 2 cần một `UtilityService` với `@Service`, một custom bean
khai báo bằng `@Bean` trong `@Configuration`, và inject bean để sử dụng.

Project có ba lớp mới:

| File | Trách nhiệm |
| --- | --- |
| `config/AppConfig.java` | Tạo bean `Clock` lấy thời gian theo múi giờ Việt Nam |
| `service/UtilityService.java` | Chuẩn hóa khoảng trắng trong tên và lấy ngày từ `Clock` |
| `controller/EmployeePreviewController.java` | Nhận request và gọi `UtilityService`, trả JSON |

Chọn `Clock` có sẵn trong Java làm ví dụ `@Bean`: không cần thư viện mới và dễ
chứng minh lợi ích của DI bằng một đồng hồ cố định trong test.

## Đối chiếu yêu cầu thực hành Lab 2

| Yêu cầu trong đề | Phần thực hiện | Kết luận |
| --- | --- | --- |
| Tạo `UtilityService` với `@Service`, có chức năng tiện ích | `formatName()` chuẩn hóa khoảng trắng trong tên | Đạt |
| Khai báo custom bean bằng `@Bean` trong lớp `@Configuration` | `AppConfig.applicationClock()` tạo bean `Clock` | Đạt |
| Inject bean vào Controller/Service của Employee Management và sử dụng | `Clock` được inject vào `UtilityService`; service được inject vào `EmployeePreviewController`; API gọi cả hai method của service | Đạt |

Format chuỗi và tạo mã nhân viên là các ví dụ chức năng trong đề, không yêu cầu
phải làm cả hai. Tương tự, `PasswordEncoder` và `ModelMapper` là các ví dụ custom
bean; `Clock` đáp ứng yêu cầu `@Bean` và đã được sử dụng thực tế trong service.

Tên `EmployeePreviewController`, method `preview()` và đường dẫn `/employees/preview`
mô tả chức năng xem trước tên đã chuẩn hóa và ngày hiện tại. Tên Lab 2 vẫn giữ
trong tài liệu học để dễ theo dõi tiến độ. Endpoint cũ `/lab2/demo` đã được thay thế.

## Chạy và quan sát

Tại thư mục project, chạy:

```sh
./mvnw spring-boot:run
```

Nếu đang chạy bản Lab 1, dừng phiên đó bằng `Ctrl+C` trước khi chạy bản mới.
Mở <http://localhost:8080/employees/preview> hoặc gọi:

```sh
curl -s http://localhost:8080/employees/preview
```

Response ví dụ (ngày thay đổi theo ngày thực tế tại Việt Nam):

```json
{
  "originalName": "  Nguyen   Duc Minh  ",
  "formattedName": "Nguyen Duc Minh",
  "currentDate": "2026-09-14"
}
```

Thử tên khác, có nhiều khoảng trắng:

```sh
curl -sG http://localhost:8080/employees/preview --data-urlencode 'name=  Tran   Thi   Lan  '
```

`formattedName` phải là `Tran Thi Lan`. `/hello` của Lab 1 vẫn hoạt động.
API này chỉ minh họa các bean đang được sử dụng; chưa lưu nhân viên.

## Bean là gì? IoC Container là gì?

**Bean** là một đối tượng được Spring IoC Container tạo hoặc nhận đăng ký và quản lý.
Không phải mọi đối tượng Java đều là bean. Ví dụ, một `new UtilityService(clock)`
do bạn tự tạo trong test là đối tượng Java thông thường.

**IoC Container** của ứng dụng là Spring ApplicationContext. Nó đọc cấu hình,
đăng ký bean và cung cấp dependency cần thiết khi tạo các bean khác.

Nếu tự nối các đối tượng trong Java, bạn có thể viết:

```java
Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
UtilityService service = new UtilityService(clock);
EmployeePreviewController controller = new EmployeePreviewController(service);
```

Trong ứng dụng này, Spring thực hiện việc nối đối tượng. Đây là **IoC — Inversion
of Control**: trách nhiệm tạo và nối các component được giao cho container.
**DI — Dependency Injection** là cách container truyền dependency vào đối tượng.

## Cách 1: đăng ký lớp do mình viết bằng @Service

```java
@Service
public class UtilityService {
    private final Clock clock;

    public UtilityService(Clock clock) {
        this.clock = clock;
    }
}
```

`@Service` là một dạng chuyên biệt của `@Component`, thể hiện đây là lớp service.
Component scanning phát hiện lớp này trong package con của lớp application.
Spring thấy constructor cần `Clock` và tìm bean phù hợp để truyền vào.

`private final` giữ reference dependency ổn định sau khi khởi tạo. Nó không tự
biến dependency thành bean và cũng không tự bảo đảm thread safety.

## Cách 2: đăng ký đối tượng bằng @Bean

```java
@Configuration
public class AppConfig {
    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
```

- `@Configuration`: đánh dấu lớp cấu hình các bean.
- `@Bean`: đăng ký đối tượng method trả về vào container.
- Bean này mặc định có tên `applicationClock`, theo tên method; kiểu là `Clock`.
- `Clock` là lớp của Java, mình không sửa nó để gắn `@Service`. `@Bean` phù hợp
  khi muốn đăng ký đối tượng từ thư viện hoặc tùy chỉnh cách tạo đối tượng.
- Múi giờ được khai báo rõ, nên việc tính ngày không phụ thuộc múi giờ mặc định của máy chạy.

`Clock` biểu diễn nguồn thời gian. Nó không đóng băng thời gian lúc khởi tạo;
`Clock.system(...)` tiếp tục đọc thời gian hiện tại mỗi khi được dùng.

## Constructor Injection: Spring truyền dependency thế nào?

```java
public EmployeePreviewController(UtilityService utilityService) {
    this.utilityService = utilityService;
}
```

Constructor nói rõ controller cần `UtilityService`. Khi tạo controller, Spring
truyền service đã được quản lý vào constructor; controller không tự tạo service.
Tương tự, Spring truyền bean `Clock` vào constructor của `UtilityService`.

Trong ví dụ này, mỗi kiểu dependency chỉ có một bean phù hợp, nên Spring chọn
được theo kiểu. Nếu sau này có nhiều bean cùng kiểu, có thể cần `@Qualifier`
hoặc `@Primary` để chọn rõ bean.

**Vì sao không có @Autowired?** Mỗi lớp chỉ có một constructor, nên Spring tự
dùng constructor đó để inject. Có thể thêm `@Autowired` vào constructor nhưng
không cần thiết trong trường hợp này. `@Autowired` yêu cầu inject dependency;
nó không tự đăng ký một lớp làm bean.

Constructor injection giúp dependency bắt buộc được thể hiện rõ và cho phép
test service bằng Java thông thường mà không khởi động toàn bộ Spring.

## Phân biệt các annotation

| Annotation | Dùng ở đâu | Vai trò |
| --- | --- | --- |
| `@Component` | Lớp | Đăng ký component thông thường qua scanning |
| `@Service` | Lớp | Component ở tầng service |
| `@Repository` | Lớp truy cập dữ liệu | Component truy cập dữ liệu; hỗ trợ cơ chế dịch exception persistence |
| `@RestController` | Lớp | Controller ghi kết quả vào response body |
| `@Configuration` | Lớp | Khai báo cấu hình bean |
| `@Bean` | Method | Đăng ký đối tượng method trả về |
| `@Autowired` | Constructor/field/method | Yêu cầu Spring inject dependency |

Lab 2 chưa có truy cập database nên chưa cần tạo repository.

## Luồng khởi tạo và luồng request

Khi ứng dụng khởi động, các quan hệ phụ thuộc được nối như sau (sơ đồ diễn giải
dependency, không phải thứ tự tạo mọi bean trong ứng dụng):

```text
AppConfig.applicationClock() → bean Clock
                                 ↓ inject qua constructor
                            UtilityService
                                 ↓ inject qua constructor
                            EmployeePreviewController
```

Khi gọi `GET /employees/preview?name=...`:

1. Spring MVC lấy tham số `name`; nếu không truyền, dùng giá trị mặc định.
2. Controller gọi `utilityService.formatName(name)`.
3. `strip()` bỏ khoảng trắng ở hai đầu; `replaceAll("\\s+", " ")` gộp chuỗi
   khoảng trắng thông thường, tab, xuống dòng thành một dấu cách.
4. Controller gọi `utilityService.currentDate()`; service dùng `LocalDate.now(clock)`.
5. Controller trả `Map`; Spring chuyển thành JSON trong HTTP response.

Các bean trên mặc định có scope **singleton**: một instance cho mỗi bean definition
trong một container. Mỗi request dùng lại controller/service đã có. Vì vậy không
lưu tên của request vào field của service: dữ liệu từng request dùng biến/tham số cục bộ.

## Test minh họa lợi ích của DI

`UtilityServiceTests` truyền một `Clock.fixed(...)` vào service. Test đặt thời điểm
18:00 UTC ngày 31/12/2020, tương ứng 01:00 ngày 01/01/2021 tại Việt Nam, rồi kiểm tra
service trả đúng ngày 01/01/2021. Nếu service bỏ qua clock được inject và gọi
`LocalDate.now()` trực tiếp, test này sẽ không còn ổn định theo thời gian.

```sh
./mvnw test
```

Test context có sẵn kiểm tra ứng dụng Spring khởi động được với các dependency mới.
Test service kiểm tra chuẩn hóa tên và cách sử dụng clock. Gọi API thực tế kiểm tra
controller sử dụng được service và trả đúng JSON.

## Các lỗi thường gặp và bài tự thử

- Bỏ `@Service`: Spring không tìm thấy `UtilityService` để tạo controller.
- Bỏ `@Bean`: Spring không tìm thấy `Clock` để tạo service, vì project chưa có bean Clock khác.
- Đặt lớp ngoài package scanning: annotation có mặt nhưng lớp vẫn không được phát hiện.
- Tự viết `new UtilityService(...)` trong controller: đối tượng tự tạo không được Spring quản lý.
- Tên chỉ có khoảng trắng sẽ thành chuỗi rỗng. Chuẩn hóa không thay thế validation;
  yêu cầu tên không rỗng sẽ được làm ở Lab 5.

Bạn hãy thử đổi tên ở query parameter và dự đoán `formattedName`. Sau đó đọc
constructor của controller/service và chỉ ra dependency của từng lớp. Có thể
thử bỏ `@Service` để xem lỗi khởi động, rồi khôi phục annotation.

Tài liệu tham khảo:
- [Spring: sử dụng @Autowired và constructor injection](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)
- [Spring: Java-based configuration](https://docs.spring.io/spring-framework/reference/core/beans/java.html)

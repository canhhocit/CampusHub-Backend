---
name: springboot-patterns
description: Spring Boot modular monolith architecture, layered service design, safe transaction boundaries, and DTO patterns for CampusHub-Backend. Use when building or reviewing controllers, services, repositories, DTOs, transactions, caching, or external integrations.
metadata:
  origin: ECC
---

# Spring Boot Development Patterns (CampusHub Architecture)

Hướng dẫn kiến trúc và mẫu thiết kế mã nguồn chuẩn cho repository CampusHub-Backend (Spring Boot 3.4.1, Java 17, PostgreSQL).

> Xem tài liệu chi tiết tại: [references/modular-monolith-guidelines.md](references/modular-monolith-guidelines.md)

---

## 1. Kiến Trúc Modular Monolith & Phân Tầng Trách Nhiệm

Mã nguồn được tổ chức theo module nghiệp vụ (`com.campushub.modules.<feature>.*`), dùng chung các tiện ích tại `com.campushub.common.*`.

### Phân tách trách nhiệm 3 tầng (Layered Separation):
- **Controller Layer**: Chỉ tiếp nhận HTTP, trích xuất token/user, validate request DTO qua `@Valid`, ủy quyền xử lý cho Service và trả về `ResponseEntity<T>`. Tuyệt đối không chứa logic tính toán nghiệp vụ hay truy vấn SQL.
- **Service Layer**: Nơi duy nhất thực thi logic nghiệp vụ, xác thực quyền sở hữu (ownership), kiểm tra điều kiện trạng thái và quản lý ranh giới giao dịch (`@Transactional`).
- **Repository Layer**: Giao tiếp PostgreSQL qua Spring Data JPA, derived queries và `@Query` có parameter binding (`:param`).

```java
// Ví dụ Controller chuẩn: Gọn gàng, chỉ ủy quyền và validate DTO
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Marketplace", description = "Quản lý sản phẩm và danh mục")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            Authentication authentication) {
        ProductResponse response = marketplaceService.createProduct(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

## 2. Thiết Kế DTO, Validation & Mapping

- Sử dụng Java 17 Records hoặc DTO chuyên biệt cho từng luồng dữ liệu (Request DTO vs Response DTO).
- Không để lọt Entity JPA trực tiếp ra ngoài API response (tránh lộ cấu trúc bảng hoặc vòng lặp quan hệ lười Lazy Loading).
- Áp dụng Bean Validation đầy đủ trên Request DTO:

```java
public record ProductCreateRequest(
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    String title,

    @NotBlank(message = "Mô tả không được để trống")
    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    String description,

    @NotNull(message = "Giá không được để trống")
    @Positive(message = "Giá phải lớn hơn 0")
    BigDecimal price,

    @NotNull(message = "Danh mục không được để trống")
    Long categoryId,

    List<@NotBlank String> detailImageUrls
) {}
```

---

## 3. Ranh Giới Giao Dịch & Quy Tắc Gọi Dịch Vụ HTTP Ngoại Vi

### 3.1. Rủi ro nghẽn Connection Pool & Kiến trúc mục tiêu
CampusHub tích hợp các dịch vụ HTTP ngoại vi: **Cloudinary** (upload media), **PayOS** (cổng thanh toán), và **OCR Service** (nhận dạng CCCD/khuôn mặt).
- **Nguyên lý kiến trúc**: Tránh giữ Database Transaction (`@Transactional`) trong lúc đang chờ phản hồi từ các cuộc gọi HTTP mạng bên ngoài. Network I/O có thể mất từ 1-5 giây; việc giữ kết nối cơ sở dữ liệu qua mạng sẽ làm chiếm dụng kết nối của HikariCP connection pool, nhanh chóng gây cạn kiệt pool và làm nghẽn toàn bộ ứng dụng.
- **Hiện trạng mã nguồn CampusHub**:
  - Trong các module hiện tại (`PaymentService.java`, `IdentityVerificationService.java`), một số phương thức vẫn đang đặt `@Transactional` bao quanh cả bước gọi bên thứ ba (OCR, PayOS).
  - Khi viết tính năng mới hoặc tối ưu hóa, hãy tách bạch quy trình theo mô hình 3 bước:
    1. **Bước 1 (Kiểm tra/Chuẩn bị)**: Đọc dữ liệu hoặc cập nhật trạng thái chuẩn bị (Transaction ngắn).
    2. **Bước 2 (Gọi HTTP ngoại vi)**: Gọi PayOS / Cloudinary / OCR client **bên ngoài Transaction**.
    3. **Bước 3 (Ghi nhận kết quả nguyên tử)**: Mở transaction ngắn gọn để lưu kết quả và cập nhật trạng thái Entity vào PostgreSQL.

### 3.2. Xử lý kịch bản thất bại Dual-Write (API ngoài thành công nhưng DB thất bại)
Khi tương tác với hệ thống phân tán bên thứ ba mà không có 2-Phase Commit (2PC):
1. **Trường hợp Cloudinary**:
   - Nếu upload ảnh/video lên Cloudinary thành công nhưng lưu URL vào DB bị lỗi -> File trở thành file mồ côi (orphaned asset) trên Cloudinary.
   - *Cách xử lý*: Chấp nhận rủi ro này (dung lượng Cloudinary có thể dọn sau), bọc upload trong `try-catch` (`log.warn(...)` như trong `IdentityVerificationService`) để lỗi tải ảnh phụ không làm đổ vỡ toàn bộ giao dịch chính.
2. **Trường hợp PayOS (Tạo link thanh toán)**:
   - Nếu PayOS đã tạo link thanh toán và mã `orderCode`, nhưng lưu DB thất bại -> Người dùng có thể thanh toán một `orderCode` mà DB chưa lưu.
   - *Cách xử lý*: Lưu hoặc sinh `payosOrderCode` trước khi gọi PayOS, hoặc sử dụng cơ chế tạo mã xác định theo `order.getId()`.
3. **Trường hợp OCR / Liveness**:
   - Nếu gọi OCR thành công nhưng lưu DB thất bại -> Người dùng nhận lỗi và thực hiện lại lượt thử (attempt) mới.
   - Bắt các ngoại lệ mạng cụ thể (`OcrConnectionException`, `OcrTimeoutException`) và dịch thành `ErrorCode.IDENTITY_SERVICE_UNAVAILABLE`, không để văng HTTP 500 không kiểm soát.

---

## 4. Quy Tắc Retry & Idempotency (Xử Lý Tác Dụng Phụ An Toàn)

### 4.1. CẤM retry cho các thao tác có side effect
- **TUYỆT ĐỐI KHÔNG tự động retry các thao tác có tác dụng phụ (side effects)**:
  - Thao tác POST tạo sản phẩm, tạo đơn hàng -> Nguy cơ tạo trùng đơn hàng.
  - Thao tác tạo thanh toán hoặc trừ tiền PayOS -> Nguy cơ tạo nhiều link thanh toán hoặc trừ tiền nhiều lần.
  - Thao tác upload Cloudinary -> Nguy cơ lãng phí dung lượng.
  - Lệnh gửi tín hiệu mở ngăn tủ thông minh -> Gửi nhiều xung lệnh làm kẹt cơ chế phần cứng.

### 4.2. Mẫu thiết kế Idempotency (Hiện thực thực tế trong `PaymentService`)
Khi nhận Webhook từ bên ngoài (ví dụ PayOS gửi webhook xác nhận thanh toán nhiều lần khi mạng chập chờn):
- **Luôn kiểm tra trạng thái hiện tại trước khi ghi nhận**:
  ```java
  // Minh họa từ PaymentService.java:
  Optional<Order> orderOpt = orderRepository.findByPayosOrderCode(orderCode);
  if (orderOpt.isEmpty()) {
      log.warn("PayOS webhook orderCode không tồn tại: {}", orderCode);
      return Map.of("code", "00", "desc", "Đã nhận nhưng không tìm thấy đơn"); // Trả 00 để PayOS không retry dồn dập
  }

  Order order = orderOpt.get();
  if (order.getPaymentStatus() != PaymentStatus.PAID) { // Kiểm tra tính bất biến idempotent
      order.setPaymentStatus(PaymentStatus.PAID);
      order.setPaymentPaidAt(LocalDateTime.now());
      orderRepository.save(order);
  }
  return Map.of("code", "00", "desc", "success");
  ```
- **Chỉ áp dụng retry khi**:
  - Thao tác là **Idempotent** (đọc GET, hoặc truy vấn kiểm tra trạng thái thanh toán `getPaymentStatus`).
  - Có khoảng trễ lũy thừa (Exponential Backoff) và giới hạn số lần thử (tối đa 2-3 lần).

---

## 5. Caching & Xử Lý Bất Đồng Bộ Đúng Chuẩn

- **Caching**:
  - Chỉ cache dữ liệu đọc thường xuyên nhưng ít thay đổi (ví dụ: Danh mục sản phẩm, cấu hình media/phí hệ thống).
  - Sử dụng **Caffeine Cache** đã có trong `CacheConfig.java` (`@Cacheable(value = "...", key = "...")`).
  - **Không tự ý thêm Redis** vào ứng dụng khi không có chỉ thị.
  - Nhớ cấu hình `@CacheEvict` khi dữ liệu danh mục hoặc cấu hình bị cập nhật.
- **Async (`@Async`)**:
  - Dùng cho các tác vụ phụ trợ không bắt buộc hoàn thành ngay lập tức (gửi email thông báo).
  - Phải cấu hình thread pool có giới hạn (tránh tạo thread vô hạn làm treo JVM).

---

## 6. Xử Lý Ngoại Lệ Nhất Quán (Exception Handling)

- Mọi lỗi nghiệp vụ phải ném `AppException(ErrorCode.<MA_LOI>)`.
- Không bắt `Exception` rồi nuốt lỗi (silent catch).
- `GlobalExceptionHandler` bắt tập trung toàn bộ ngoại lệ và trả về mã lỗi chuẩn hóa, không trả về raw stack trace ra ngoài response.

```java
// Ném ngoại lệ nghiệp vụ chuẩn
if (product.getStatus() != ProductStatus.ACTIVE) {
    throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE);
}
```

---

## 7. Nguyên Tắc Tránh Over-Engineering & Mặc Định Sản Xuất

- **Ưu tiên giải pháp đơn giản**: Không tự ý tạo thêm abstract class, factory, generic wrapper hay module mới nếu nghiệp vụ thực tế chưa yêu cầu.
- **Constructor Injection**: Sử dụng constructor injection, tránh `@Autowired` trên field.
- **Optimistic Locking**: Sử dụng trường `@Version` trên Entity để phát hiện và ngăn chặn ghi đè đồng thời (`OptimisticLockException` -> HTTP 409 Conflict).
- **Kiến trúc bền vững**: Giữ vững mô hình Modular Monolith, không tự phân tách thành microservices hay đưa vào các message queue (Kafka, RabbitMQ) không cần thiết.

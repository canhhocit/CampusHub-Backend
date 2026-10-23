# CampusHub Modular Monolith Architecture & Design Guidelines

Tài liệu tham chiếu chuẩn về tổ chức mã nguồn, phân tầng kiến trúc và quản lý transaction trong repository `CampusHub-Backend`.

---

## 1. Cấu Trúc Gói Modular Monolith

CampusHub được tổ chức theo mô hình **Modular Monolith** định hướng nghiệp vụ (Feature-Oriented):

```
com.campushub
├── common/                  # Các thành phần dùng chung toàn hệ thống
│   ├── bootstrap/           # Khởi tạo dữ liệu mẫu ban đầu (admin, category)
│   ├── config/              # Cấu hình Spring (CORS, Cache Caffeine, WebMvc)
│   ├── email/               # Dịch vụ gửi email (JavaMailSender)
│   ├── enums/               # Enum dùng chung toàn hệ thống (Role, etc.)
│   ├── exception/           # AppException, ErrorCode, GlobalExceptionHandler
│   ├── model/               # BaseEntity (createdAt, updatedAt)
│   └── security/            # SecurityConfig, JwtAuthFilter, JwtProvider
├── infrastructure/          # Client giao tiếp với hệ thống bên ngoài
│   └── ocr/                 # OcrServiceClient, DTOs, OcrProperties
└── modules/                 # Các module nghiệp vụ độc lập
    ├── admin/               # Quản trị hệ thống, cấu hình tham số, phí
    ├── audit/               # Ghi vết nhật ký kiểm toán (AuditLog)
    ├── auth/                # Đăng ký, đăng nhập, xác thực email, refresh token
    ├── identity/            # Xác minh danh tính CCCD (eKYC, đối soát OCR)
    ├── locker/              # Tủ thông minh, ngăn tủ, mã PIN, điều khiển cửa
    ├── marketplace/         # Sản phẩm, danh mục, tìm kiếm, đăng bán/cho thuê
    ├── order/               # Đơn hàng mua/thuê, vòng đời đơn hàng, ký gửi tủ
    ├── payment/             # Tích hợp cổng PayOS, xử lý webhook thanh toán
    ├── ping/                # Endpoint kiểm tra sức khỏe hệ thống (healthcheck)
    ├── upload/              # Tải lên hình ảnh lên Cloudinary
    └── user/                # Quản lý hồ sơ người dùng, phân quyền
```

---

## 2. Phân Tầng Trách Nhiệm (Layered Architecture)

### 2.1. Controller Layer (`controller`)
- Tiếp nhận HTTP Request, trích xuất parameters, headers và `@AuthenticationPrincipal` hoặc `Authentication`.
- Xác thực cú pháp đầu vào qua `@Valid` trên Request DTO.
- Ủy quyền toàn bộ xử lý nghiệp vụ cho Service Layer.
- Trả về `ResponseEntity<T>` với HTTP status code phù hợp (`200 OK`, `201 CREATED`, `204 NO_CONTENT`).
- **Không chứa business logic**: Không tính toán tiền, không kiểm tra điều kiện trạng thái đơn hàng, không truy vấn database trực tiếp.

### 2.2. Service Layer (`service`)
- Nơi duy nhất chứa quy tắc nghiệp vụ (Business Invariants).
- Kiểm tra tính hợp lệ nghiệp vụ và xác thực quyền sở hữu tài nguyên (Server-side Ownership).
- Xác lập ranh giới giao dịch (`@Transactional`).
- Gọi các Repository để truy xuất/lưu trữ dữ liệu.
- Chuyển đổi giữa Entity và DTO (Mapping).
- **Không over-engineer**: Không tạo Interface rỗng nếu service chỉ có 1 implementation duy nhất và không có nhu cầu đa hình (trừ khi dự án đã theo quy ước `Interface` + `ServiceImpl` sẵn có của module đó).

### 2.3. Repository Layer (`repository`)
- Kế thừa `JpaRepository<Entity, ID>` của Spring Data JPA.
- Định nghĩa các derived query methods hoặc `@Query` JPQL/Native SQL.
- Sử dụng parameter binding (`:param`), không nối chuỗi.
- Sử dụng locking có chủ đích:
  - `findByIdWithLock(id)` với `@Lock(LockModeType.PESSIMISTIC_WRITE)` khi cần chống race condition cập nhật số lượng/trạng thái ngăn tủ/tiền tệ.
  - `@Version` trên Entity để áp dụng Optimistic Locking cho các cập nhật thông tin chung.

---

## 3. Ranh Giới Giao Dịch & Gọi Dịch Vụ HTTP Ngoại Vi

### 3.1. Nguy cơ nghẽn DB Connection Pool & Hiện trạng
CampusHub tích hợp nhiều dịch vụ HTTP bên thứ ba:
- **Cloudinary**: Tải lên hình ảnh/video sản phẩm và CCCD.
- **PayOS**: Tạo liên kết thanh toán, kiểm tra trạng thái giao dịch ngân hàng.
- **OCR Service**: Gọi nhận dạng CCCD và kiểm tra liveness khuôn mặt.

- **Nguyên tắc kiến trúc**: Tránh giữ Database Transaction (`@Transactional`) trong lúc đang chờ phản hồi từ các cuộc gọi HTTP mạng bên ngoài. Giữ transaction qua mạng (network I/O) có thể mất từ 1-5 giây, làm chiếm dụng kết nối HikariCP, nhanh chóng dẫn đến cạn kiệt Connection Pool và làm nghẽn ứng dụng.
- **Hiện trạng mã nguồn**:
  - Trong một số service hiện tại (`IdentityVerificationService`, `PaymentService`), các phương thức nghiệp vụ vẫn đặt `@Transactional` bao quanh cuộc gọi bên thứ ba.
  - Khi phát triển tính năng mới hoặc tái cấu trúc, khuyến nghị tuân thủ quy trình chuẩn 3 bước:
    1. **Bước 1 (Pre-call validation / read)**: Đọc dữ liệu hoặc kiểm tra điều kiện ban đầu (nếu cần dùng `@Transactional(readOnly = true)` và đóng ngay).
    2. **Bước 2 (External HTTP Call)**: Gọi HTTP API ngoại vi (PayOS, Cloudinary, OCR) **bên ngoài Transaction**.
    3. **Bước 3 (Atomic DB Write)**: Mở transaction ngắn, nguyên tử (`@Transactional`) để lưu kết quả hoặc cập nhật trạng thái Entity vào PostgreSQL.

### 3.2. Xử lý kịch bản thất bại Dual-Write (API ngoài thành công nhưng DB thất bại)
1. **Cloudinary Upload**:
   - Nếu ảnh đã upload thành công lên CDN nhưng DB rollback -> Ảnh thành tài sản mồ côi (orphaned).
   - *Giải pháp*: Bọc upload trong khối `try-catch` (`log.warn(...)`), không để lỗi upload ảnh phụ làm rollback toàn bộ giao dịch chính của người dùng.
2. **PayOS Thanh toán**:
   - Nếu link PayOS được tạo với `orderCode`, nhưng ghi DB thất bại -> Webhook sau này không đối soát được.
   - *Giải pháp*: Luôn sinh hoặc lưu `orderCode` xác định theo `order.getId()` trước khi gọi PayOS, và xử lý webhook idempotent (`findByPayosOrderCode`).
3. **OCR / Liveness Extraction**:
   - Bắt các ngoại lệ mạng `OcrConnectionException`, `OcrTimeoutException` và chuyển hóa thành mã lỗi thân thiện `ErrorCode.IDENTITY_SERVICE_UNAVAILABLE`.

---

## 4. Quy Tắc Retry & Idempotency (Xử Lý Tác Dụng Phụ An Toàn)

### 4.1. CẢNH BÁO: Cấm retry tự động cho thao tác có side effect
- **TUYỆT ĐỐI KHÔNG tự động retry các thao tác có tác dụng phụ (side effects)**:
  - Thao tác POST tạo sản phẩm, tạo đơn hàng -> Nguy cơ tạo trùng đơn hàng.
  - Thao tác tạo thanh toán PayOS -> Nguy cơ tạo nhiều link thanh toán trùng lặp.
  - Thao tác upload Cloudinary -> Nguy cơ lãng phí dung lượng.
  - Thao tác điều khiển mở khóa tủ thông minh -> Gửi nhiều xung lệnh làm kẹt phần cứng hoặc xung đột trạng thái.

### 4.2. Mẫu thiết kế Idempotency Webhook (Tham chiếu `PaymentService.java`)
- Khi tiếp nhận Webhook bên thứ ba (như PayOS thông báo thanh toán thành công):
  - Kiểm tra điều kiện trạng thái hiện tại: `if (order.getPaymentStatus() != PaymentStatus.PAID)` trước khi cập nhật.
  - Nếu đơn hàng đã ở trạng thái `PAID`, bỏ qua việc cập nhật nhưng **vẫn trả về mã thành công** (ví dụ `Map.of("code", "00", "desc", "success")`) để bên gửi webhook ngừng việc gửi lại (retry loop).
- **Chỉ áp dụng retry khi**:
  - Thao tác là **Idempotent** (đọc dữ liệu GET, hoặc kiểm tra trạng thái đơn qua `getPaymentStatus`).
  - Sử dụng chiến lược Exponential Backoff với giới hạn số lần thử (tối đa 2-3 lần) và timeout rõ ràng.

---

## 5. Caching & Xử Lý Bất Đồng Bộ Đúng Chuẩn

- **Caching**:
  - Chỉ áp dụng caching cho dữ liệu đọc nhiều nhưng ít thay đổi (Danh mục sản phẩm, cấu hình phí hệ thống, cấu hình media).
  - Sử dụng **Caffeine Cache** đã được cấu hình trong `CacheConfig.java`.
  - Không tự thêm Redis vào dự án khi chưa có yêu cầu về phân tán từ kiến trúc sư trưởng.
  - Luôn cấu hình dọn dẹp cache (`@CacheEvict`) khi dữ liệu gốc bị cập nhật.
- **Async (`@Async`)**:
  - Sử dụng cho các tác vụ nền không ảnh hưởng đến phản hồi trực tiếp của người dùng (ví dụ gửi email xác thực).
  - Bắt buộc cấu hình Executor có giới hạn hàng đợi và thread pool (không dùng `SimpleAsyncTaskExecutor` mặc định gây tạo thread không kiểm soát).

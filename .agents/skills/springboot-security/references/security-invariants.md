# CampusHub Backend Security Invariants & Threat Defenses

Tài liệu tham chiếu chuyên sâu về các nguyên tắc bảo mật, bất biến nghiệp vụ và mô hình phòng thủ rủi ro trong repository `CampusHub-Backend`.

---

## 1. Authentication vs Authorization & Server-Side Ownership

### 1.1. Phân biệt rõ Authentication và Authorization
- **Authentication (Xác thực)**: Trả lời câu hỏi *"Người gọi là ai?"*. Được xử lý bởi `JwtAuthFilter`, `JwtProvider`, và `UserDetailsServiceImpl`. Kết quả là `UserPrincipal` hoặc `Authentication` được nạp vào `SecurityContextHolder`.
- **Authorization (Phân quyền & Kiểm soát quyền hạn)**: Trả lời câu hỏi *"Người gọi có quyền thực hiện hành động này trên tài nguyên này hay không?"*. 
- **Cảnh báo cốt lõi**:
  - `@PreAuthorize("isAuthenticated()")` **CHỈ** chứng minh người dùng đã đăng nhập với token hợp lệ. Nó **KHÔNG** chứng minh người dùng có quyền sửa/xóa/đọc tài nguyên cụ thể.
  - `@PreAuthorize("hasRole('ADMIN')")` chỉ kiểm tra vai trò quản trị viên.

### 1.2. Phòng thủ IDOR (Insecure Direct Object Reference)
- Không bao giờ tin tưởng `id` (product ID, order ID, locker ID, compartment ID, user ID) gửi từ client.
- Kiểm tra quyền sở hữu bắt buộc phải thực hiện ở tầng **Service** hoặc truy vấn cơ sở dữ liệu có gắn điều kiện chủ sở hữu:
  ```java
  // Đúng: Kiểm tra sở hữu ở Service Layer trước khi cập nhật
  Product product = productRepository.findById(productId)
          .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

  if (!product.getOwner().getUsername().equals(currentUsername) && !isAdmin(currentUser)) {
      throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
  }
  ```
- Hoặc truy vấn trực tiếp có ràng buộc chủ sở hữu để ngăn chặn race condition (TOCTOU):
  ```java
  Product product = productRepository.findByIdAndOwnerId(productId, currentUserId)
          .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
  ```
- Tuyệt đối không chỉ ẩn nút trên giao diện React / Vite; kẻ tấn công có thể gọi trực tiếp API qua Postman/curl.

---

## 2. Chiến Lược CSRF Trong Kiến Trúc Token & Cookie Kết Hợp
 
### 2.1. Phân tích hiện trạng kiến trúc CampusHub
- Trong `SecurityConfig.java`:
  - Cơ chế phiên làm việc là `SessionCreationPolicy.STATELESS`.
  - Cấu hình `csrf(csrf -> csrf.disable())` được áp dụng cho toàn bộ HTTP filter chain.
- **Tại sao cấu hình này là an toàn và phù hợp trong hiện tại?**:
  - **Access Token**: Được client lưu trữ trong bộ nhớ (in-memory) và gửi qua header HTTP `Authorization: Bearer <token>`. Do trình duyệt không bao giờ tự động đính kèm Authorization header tùy ý trong các yêu cầu cross-origin, các endpoint nghiệp vụ này có khả năng miễn nhiễm tự nhiên trước các đợt tấn công CSRF.
  - **Refresh Token Cookie**: Là thành phần duy nhất sử dụng cookie tại `/api/v1/auth/refresh` và `/api/v1/auth/logout`.
    - Cookie được thiết lập với cờ `HttpOnly(true)` (ngăn XSS), `SameSite("Lax")` (chống gửi kèm tự động trong các cross-site POST), và cờ `secure(cookieSecure)` trên HTTPS.
    - CORS được cấu hình chặt chẽ với danh sách origin rõ ràng (`app.cors.allowed-origins`), chỉ cho phép các method cụ thể và bật `allowCredentials(true)`.
- **CẢNH BÁO QUAN TRỌNG**:
  - Tuyệt đối không bật CSRF trong `SecurityConfig` mà không có sự đồng bộ với Frontend React/Vite. Nếu bật CSRF mà frontend không gửi `X-XSRF-TOKEN`, toàn bộ các request POST, PUT, DELETE từ người dùng sẽ bị Spring Security chặn lại với mã lỗi HTTP 403 Forbidden.
  - Nếu hệ thống chuyển hướng sang xác thực hoàn toàn bằng Cookie cho các thao tác thay đổi dữ liệu (state-changing mutations), khi đó mới cấu hình `CookieCsrfTokenRepository.withHttpOnlyFalse()` và yêu cầu frontend gửi kèm token qua header `X-XSRF-TOKEN`.

---

## 3. Input Validation, DTO Separation & Anti-Mass Assignment

### 3.1. Phân tách DTO nghiêm ngặt
- Không bao giờ nhận Entity JPA trực tiếp tại Controller `@RequestBody`.
- Mỗi hành động (Tạo, Cập nhật, Đổi trạng thái) phải có DTO riêng biệt:
  - `ProductCreateRequest`
  - `ProductUpdateRequest`
  - `ProductStatusUpdateRequest`
- Điều này ngăn chặn triệt để lỗ hổng **Mass Assignment** (ví dụ client cố tình gửi các trường nhạy cảm như `role`, `status`, `balance`, `isVerified`, `ownerId`).

### 3.2. Ràng buộc Validation
- Áp dụng Bean Validation trên DTO:
  - Chuỗi: `@NotBlank`, `@Size(min = ..., max = ...)`
  - Số/Giá: `@NotNull`, `@Positive`, `@PositiveOrZero`, `@Digits`
  - Enum: kiểm tra giá trị enum hợp lệ, không tự ý parse chuỗi lạ
  - File/Media: kiểm tra định dạng MIME hợp lệ, dung lượng tối đa (ví dụ 10MB cho ảnh), không tin cậy đuôi mở rộng file do người dùng đặt.

---

## 4. Privacy-Safe Logging & Bảo Vệ Dữ Liệu Cá Nhân (PII)

### 4.1. Danh mục dữ liệu tuyệt đối không ghi vào Log
- Mật khẩu người dùng (kể cả plaintext hoặc mã hash).
- JWT Access Token, Refresh Token, Reset Password Token, Email Verification Token.
- Số Căn cước công dân (CCCD / CMND / Citizen ID).
- Dữ liệu sinh trắc học, vector khuôn mặt, ảnh chụp 2 mặt CCCD, ảnh selfie chân dung (trong `IdentityVerificationService`).
- Dữ liệu thẻ ngân hàng, tài khoản thanh toán PayOS webhook secret hoặc PayOS API Key / Checksum Key (trong `PayOSService` chỉ log `clientId.substring(0, Math.min(6, clientId.length()))***`).

### 4.2. Ngoại lệ phát triển được bảo lưu
- Theo Quy tắc dự án CampusHub (Rule #5): Các bản ghi log nội dung email và liên kết xác thực/reset mật khẩu phục vụ môi trường kiểm thử/phát triển local được giữ nguyên trong `EmailService.java` (`[EMAIL DISPATCH] To: ... | Subject: ... | Body/Link: ...`) để phục vụ debug, không tự ý xóa bỏ hoặc refactor khi chưa có yêu cầu rõ ràng.

### 4.3. Xử lý Exception an toàn
- `GlobalExceptionHandler` bắt tập trung toàn bộ ngoại lệ.
- Phản hồi gửi về client phải sử dụng mã lỗi nghiệp vụ chuẩn hóa (`ErrorCode`), không làm rò rỉ:
  - Stack trace hoặc tên class nội bộ.
  - Cấu trúc bảng hoặc tên cột SQL (ví dụ từ `DataIntegrityViolationException`).
  - Đường dẫn file hệ thống hoặc thông tin máy chủ.

---

## 5. Chiến Lược Giới Hạn Tần Suất (Rate Limiting)

### 5.1. Rủi ro của `ConcurrentHashMap` trong bộ nhớ
- Dễ gây rò rỉ bộ nhớ (Out-Of-Memory) nếu kẻ tấn công gửi hàng triệu IP giả mạo vì Map không tự động giải phóng key cũ.
- Bị xóa sạch khi ứng dụng khởi động lại hoặc redeploy.
- Không chia sẻ được giữa các instance nếu ứng dụng mở rộng.

### 5.2. Giải pháp thực tế trong CampusHub
- Sử dụng Spring Cache abstraction với **Caffeine Cache** (`CacheConfig.java`):
  - Giới hạn kích thước tối đa (`maximumSize = 10000`).
  - Tự động hết hạn (`expireAfterWrite = 15, TimeUnit.MINUTES`).
  - Thao tác đếm nguyên tử & an toàn concurrency (`AtomicInteger.incrementAndGet()` kết hợp `cache.putIfAbsent(key, newCounter)`).
- Áp dụng tại các điểm nhạy cảm: Đăng nhập (`rateLimitLogin` - tối đa 10 lần), Đăng ký (`rateLimitRegister` - tối đa 5 lần), Quên mật khẩu (`rateLimitForgotPassword` - tối đa 3 lần).
- **Giới hạn thực tế cần lưu ý**:
  1. *Đơn node (Single-Instance Monolith)*: Do chạy trên JVM RAM, các bộ đếm không chia sẻ giữa các server nếu mở rộng đa node. Đây là chủ đích kiến trúc của CampusHub nhằm giữ hệ thống gọn gàng, không phụ thuộc Redis. Tuyệt đối KHÔNG tự ý thêm Redis.
  2. *Truy vết IP Client*: `request.getRemoteAddr()` cần được cấu hình qua `server.forward-headers-strategy=framework` khi đứng sau Reverse Proxy/Load Balancer để tránh gộp chung địa chỉ IP của proxy.


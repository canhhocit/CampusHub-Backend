---
name: springboot-security
description: Spring Security best practices for authn/authz, ownership checks, CSRF, secrets, rate limiting, and privacy-safe logging in CampusHub-Backend. Use when reviewing or adding authentication, authorization, input validation, endpoints, or secret handling.
metadata:
  origin: ECC
---

# Spring Boot Security Review & Hardening

Hướng dẫn bảo mật chuyên sâu cho repository CampusHub-Backend (Spring Boot 3.4.1, Spring Security 6, Java 17).

> Xem tài liệu chi tiết tại: [references/security-invariants.md](references/security-invariants.md)

---

## 1. Authentication vs Authorization & Server-Side Ownership

### 1.1. Nguyên tắc cốt lõi
- **Authentication (Xác thực)**: Xác định danh tính người gọi qua JWT Bearer token (`JwtAuthFilter`).
- **Authorization (Phân quyền)**: Xác định người gọi có quyền thực hiện hành động trên đối tượng cụ thể hay không.
- **CẢNH BÁO**: `@PreAuthorize("isAuthenticated()")` **CHỈ** kiểm tra người dùng đã đăng nhập. Nó **KHÔNG** chứng minh người dùng sở hữu tài nguyên được yêu cầu. Không bao giờ dựa vào frontend để ẩn nút hay chặn request.

### 1.2. Kiểm tra quyền sở hữu tài nguyên bắt buộc ở Server-Side (Chống IDOR)
Mọi thao tác sửa đổi hoặc truy cập tài nguyên của người dùng phải xác thực quyền sở hữu trực tiếp tại Service hoặc bằng truy vấn database gắn chặt `ownerId`:

```java
// Ví dụ kiểm tra quyền sở hữu tại Service (Tránh IDOR)
@Transactional
public ProductResponse updateProduct(Long id, ProductUpdateRequest request, String currentUsername) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

    // Bắt buộc xác thực quyền sở hữu trên Server
    if (!product.getOwner().getUsername().equals(currentUsername) && !isAdmin(currentUsername)) {
        throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
    }

    // Áp dụng optimistic locking ngăn ghi đè đồng thời
    if (!product.getVersion().equals(request.getVersion())) {
        throw new AppException(ErrorCode.CONCURRENT_UPDATE);
    }

    product.setTitle(request.getTitle());
    product.setPrice(request.getPrice());
    return mapProduct(productRepository.save(product));
}
```

---

## 2. Chiến Lược CSRF & Quản Lý Cookie Thực Tế

### 2.1. Phân tích hiện trạng CSRF trong CampusHub
- Trong `SecurityConfig.java`, cấu hình hiện tại là `csrf(csrf -> csrf.disable())`.
- **Lý do kỹ thuật**:
  - Toàn bộ các API nghiệp vụ chính sử dụng xác thực phi trạng thái (**Stateless JWT**) qua HTTP Header `Authorization: Bearer <token>`. Vì trình duyệt không tự động đính kèm Authorization header trong các yêu cầu cross-origin, các endpoint này không có nguy cơ bị tấn công CSRF cổ điển.
  - **CẢNH BÁO**: Tuyệt đối KHÔNG tự ý xóa bỏ `csrf.disable()` trong `SecurityConfig` mà không có sự phối hợp với frontend; việc bật CSRF mặc định sẽ ngay lập tức làm gãy toàn bộ các request POST/PUT/DELETE từ ứng dụng React/Vite (lỗi HTTP 403 Forbidden).
- **Cơ chế bảo vệ Refresh Token Cookie**:
  - Refresh token là thành phần duy nhất được truyền qua Cookie tại `/api/v1/auth/refresh` và `/api/v1/auth/logout`.
  - Cookie này được bảo vệ đa tầng:
    1. `HttpOnly(true)`: Ngăn chặn mã độc JavaScript/XSS đọc trộm token.
    2. `SameSite("Lax")`: Ngăn trình duyệt tự động gửi cookie trong các POST request từ trang web bên ngoài.
    3. `secure(cookieSecure)`: Bật `true` trên môi trường Production HTTPS (cấu hình qua `app.jwt.cookie-secure`).
    4. **CORS nghiêm ngặt**: Chỉ cho phép danh sách origin cụ thể trong cấu hình (`app.cors.allowed-origins`), không sử dụng wildcard `*` khi `allowCredentials(true)`.
- **Khi nào cần kích hoạt CSRF?**:
  - Chỉ kích hoạt CSRF protection nếu kiến trúc chuyển dịch sang sử dụng Cookie để xác thực cho toàn bộ các endpoint thay đổi trạng thái (mutation). Khi đó, bắt buộc cấu hình `CookieCsrfTokenRepository.withHttpOnlyFalse()` và frontend phải gửi lại header `X-XSRF-TOKEN`.

---

## 3. Input Validation & Chống Mass Assignment

### 3.1. Tách biệt DTO với Entity
- Tuyệt đối không nhận Entity JPA làm tham số `@RequestBody` tại Controller.
- Sử dụng Java 17 Records hoặc DTO chuyên biệt cho từng hành động để ngăn kẻ tấn công chèn các trường nhạy cảm (`role`, `status`, `owner`, `balance`, `isVerified`).

```java
// DTO chỉ chứa các trường người dùng ĐƯỢC PHÉP nhập
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

### 3.2. Kiểm duyệt File Upload
- Luôn kiểm tra định dạng MIME thực tế, đuôi file cho phép và kích thước file tối đa (ví dụ 10MB cho ảnh) trước khi xử lý hoặc tải lên Cloudinary.

---

## 4. Chống SQL Injection

- Sử dụng Spring Data JPA derived queries hoặc query có tham số (`:param`).
- Tuyệt đối không nối chuỗi (`+`) vào câu lệnh SQL native:

```java
// NGUY HIỂM: Nối chuỗi SQL dẫn đến SQL Injection
@Query(value = "SELECT * FROM products WHERE title LIKE '%" + search + "%'", nativeQuery = true)

// AN TOÀN: Parameter binding tự động escape
@Query("SELECT p FROM Product p WHERE p.status = :status AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(:search))")
Page<Product> searchProducts(@Param("status") ProductStatus status, @Param("search") String search, Pageable pageable);
```

---

## 5. Mật Khẩu, Secrets & Phản Hồi Lỗi An Toàn

### 5.1. Quản lý mật khẩu & Secrets
- Luôn mã hóa mật khẩu bằng `BCryptPasswordEncoder` (cost factor tối thiểu 10-12).
- Không lưu trữ credentials, token bí mật hay API key trong mã nguồn hoặc commit lên Git.
- Sử dụng biến môi trường trong `application.yml`: `${DB_PASSWORD}`, `${JWT_SECRET}`, `${PAYOS_API_KEY}`.
- Khi ghi log cấu hình bên ngoài, phải che (mask) secrets (ví dụ `PayOSService` che `clientId.substring(0, Math.min(6, clientId.length()))***`).

### 5.2. Phản hồi lỗi an toàn (Safe Error Responses)
- Không bao giờ trả về stack trace, thông tin bảng SQL, hoặc đường dẫn file nội bộ ra response của API.
- Tập trung hóa xử lý lỗi qua `@RestControllerAdvice` (`GlobalExceptionHandler`) với cấu trúc chuẩn:
  ```json
  {
    "error": "UNAUTHORIZED_ACTION",
    "message": "Bạn không có quyền chỉnh sửa tài nguyên này"
  }
  ```

---

## 6. Privacy-Safe Logging (Bảo Vệ Dữ Liệu Riêng Tư)

- **TUYỆT ĐỐI KHÔNG GHI LOG**:
  - Mật khẩu, mã OTP, Access Token, Refresh Token, Reset Password Token.
  - Số Căn cước công dân (CCCD), ảnh 2 mặt CCCD, ảnh selfie nhận diện khuôn mặt, vector sinh trắc học (như trong `IdentityVerificationService`).
  - Thông tin thẻ ngân hàng hoặc chi tiết thanh toán nhạy cảm (PayOS API Key, Checksum Key).
- **Ngoại lệ bảo lưu**: Giữ nguyên cơ chế log nội dung email và liên kết xác thực dùng cho môi trường phát triển/debug local theo quy tắc dự án CampusHub (Rule #5 trong `EmailService.java`: `[EMAIL DISPATCH] To: ... | Subject: ... | Body/Link: ...`).

---

## 7. Giới Hạn Tần Suất (Rate Limiting) Đúng Kiến Trúc

### 7.1. Hiện trạng cài đặt trong CampusHub
- Hệ thống sử dụng **Caffeine Cache** tích hợp qua Spring Cache abstraction (`CacheConfig.java`):
  - Tham số cấu hình: `expireAfterWrite(15, TimeUnit.MINUTES)`, `maximumSize(10000)`.
  - Các vùng cache được định nghĩa: `rateLimitLogin` (10 lần), `rateLimitRegister` (5 lần), `rateLimitForgotPassword` (3 lần).
- **Thao tác đếm nguyên tử & an toàn luồng (Concurrency)**:
  `AuthServiceImpl.java` xử lý atomic concurrency qua `cache.putIfAbsent(key, newCounter)` kết hợp với `AtomicInteger.incrementAndGet()`, tránh race condition khi hai request cùng tới một thời điểm:
  ```java
  Cache.ValueWrapper wrapper = cache.get(key);
  AtomicInteger count;
  if (wrapper == null) {
      AtomicInteger newCounter = new AtomicInteger(0);
      Cache.ValueWrapper existing = cache.putIfAbsent(key, newCounter);
      count = (existing == null) ? newCounter : (AtomicInteger) existing.get();
  } else {
      count = (AtomicInteger) wrapper.get();
  }
  if (count != null && count.incrementAndGet() > maxRequests) {
      throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
  }
  ```

### 7.2. Giới hạn thực tế & Lưu ý môi trường
1. **Phạm vi bộ nhớ (Node-Local Memory)**:
   - Caffeine lưu trong RAM của JVM hiện tại. Khi ứng dụng chạy 1 instance (Modular Monolith), giải pháp này cực kỳ hiệu quả, nhẹ và không cần Redis.
   - Nếu trong tương lai triển khai cụm đa node (multi-instance) sau Load Balancer mà không có sticky session, bộ đếm sẽ độc lập trên từng node. (Duy trì Caffeine theo rule dự án, KHÔNG tự ý cài đặt Redis).
2. **Nhận diện Client IP**:
   - `getClientIp` hiện lấy trực tiếp từ `request.getRemoteAddr()`. Khi triển khai sau Reverse Proxy (Nginx/Cloudflare), cần đảm bảo cấu hình `server.forward-headers-strategy=framework` hoặc `ForwardedHeaderFilter` để tránh toàn bộ người dùng bị tính chung IP của proxy.

---

## 8. Kiểm Thử Quyền Hạn (Authorization & Security Testing)

Mọi endpoint nhạy cảm phải có test chứng minh người không đủ quyền sẽ bị chặn:

```java
@Test
@WithMockUser(username = "attacker", roles = "USER")
void updateProduct_whenNotOwner_returnsUnauthorizedAction() throws Exception {
    mockMvc.perform(put("/api/v1/products/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Hacked Title\",\"price\":100000,\"version\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED_ACTION"));
}
```

---

## 9. Checklist Rà Soát Trước Khi Xuất Bản

- [ ] Phân biệt rõ giữa kiểm tra đăng nhập (`isAuthenticated()`) và kiểm tra sở hữu tài nguyên (Server-side ownership).
- [ ] Mọi endpoint chỉnh sửa/xóa tài nguyên đều có xác thực chủ sở hữu hoặc phân quyền Admin.
- [ ] Giữ nguyên `csrf.disable()` phù hợp với Stateless JWT Bearer, không bật bừa bãi làm vỡ frontend.
- [ ] Refresh token cookie có đầy đủ `HttpOnly`, `SameSite=Lax`, và `secure(true)` trên production.
- [ ] Không có nối chuỗi SQL hoặc native query thiếu parameter binding.
- [ ] DTO request tách biệt hoàn toàn với Entity, có Bean Validation đầy đủ.
- [ ] Không lộ stack trace hay thông tin nhạy cảm qua `GlobalExceptionHandler`.
- [ ] Không có mật khẩu, token, số CCCD hoặc dữ liệu sinh trắc học bị ghi vào log.
- [ ] Giữ nguyên email debug logs phục vụ phát triển (Rule #5).
- [ ] Rate limiting sử dụng Caffeine với atomic counter, không dùng `ConcurrentHashMap` unbounded và không tự ý thêm Redis.

# CampusHub Backend Testing Patterns & Strategies

Tài liệu tham chiếu chuyên sâu về chiến lược kiểm thử, kiểm thử bảo mật quyền hạn và tính toàn vẹn cơ sở dữ liệu trong repository `CampusHub-Backend`.

---

## 1. Kim Tự Tháp Kiểm Thử Trong CampusHub

Để kiểm thử hiệu quả, chi phí thấp và độ tin cậy cao, dự án phân chia rõ 3 tầng test:

```
           / \
          /   \     Integration & Regression Tests (@SpringBootTest)
         /     \    - Kiểm tra luồng liên module (Order -> Payment -> Locker)
        /-------\   - Chạy với H2 in-memory test profile
       /         \
      /  WebMvc   \ Web & Authorization Tests (@WebMvcTest / MockMvc)
     /   Security  \ - HTTP status, DTO validation, @PreAuthorize, IDOR checks
    /---------------\
   /                 \ Unit Tests (JUnit 5 + Mockito)
  /     Unit Tests    \ - Kiểm tra pure business logic, tính toán, trạng thái
 /                     \ - Chạy siêu nhanh, không load Spring context
-------------------------
```

---

## 2. Unit Tests: Kiểm Thử Logic Nghiệp Vụ Thuần Túy

- **Mục tiêu**: Kiểm tra các quy tắc nghiệp vụ phức tạp, máy trạng thái (state machines), tính toán tiền cọc/tiền thuê, kiểm tra điều kiện logic.
- **Công cụ**: JUnit 5 Jupiter, Mockito (`@ExtendWith(MockitoExtension.class)`), AssertJ.
- **Quy tắc**:
  - Không khởi động Spring Context (không dùng `@SpringBootTest` cho unit test).
  - Mock toàn bộ các dependencies ngoại vi (Repositories, EmailService, OcrServiceClient).
  - Áp dụng cấu trúc **Arrange - Act - Assert**.
  - Kiểm tra đầy đủ luồng thành công (Happy Path) và các nhánh ngoại lệ (`assertThatThrownBy`).

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private OrderService orderService;

    @Test
    void createOrder_whenProductNotActive_throwsAppException() {
        // Arrange
        User buyer = new User();
        buyer.setUsername("buyer");

        Product product = new Product();
        product.setStatus(ProductStatus.SOLD); // Không hoạt động
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(1L, buyer.getUsername()))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_AVAILABLE);

        verify(orderRepository, never()).save(any());
    }
}
```

---

## 3. Web & Security Tests: Kiểm Thử Quyền Hạn & Ràng Buộc DTO

- **Mục tiêu**: Xác nhận bộ lọc bảo mật, `@PreAuthorize`, xử lý ngoại lệ và ràng buộc `@Valid` trên DTO.
- **Tính tương thích Spring Boot 3.4.1**:
  - Codebase CampusHub hiện đang sử dụng `@MockBean` (`org.springframework.boot.test.mock.mockito.MockBean`) như trong `IdentityIntegrationTest.java`.
  - Từ Spring Boot 3.4, annotation `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`) được giới thiệu thay thế. Cả hai đều hoạt động tốt trên Spring Boot 3.4.1. Không tự ý refactor phá vỡ các test đang có.
- **Kiểm thử IDOR & Quyền sở hữu (Ownership Tests)**:
  Bắt buộc kiểm tra 4 trường hợp cho mọi endpoint chỉnh sửa tài nguyên:
  1. **Khách vô danh (Anonymous)**: Gọi API không có token -> Mong đợi `401 Unauthorized` (`{"status":401,"error":"Unauthorized",...}`).
  2. **Người dùng khác (Attacker / Non-Owner)**: Người dùng có token nhưng không sở hữu tài nguyên -> Mong đợi `403 Forbidden` hoặc `400 BAD REQUEST` với lỗi `UNAUTHORIZED_ACTION`.
  3. **Chủ sở hữu hợp pháp (Owner)**: Người tạo tài nguyên -> Mong đợi `200 OK`.
  4. **Quản trị viên (Admin)**: Có role `ROLE_ADMIN` -> Mong đợi `200 OK` (nếu nghiệp vụ cho phép admin can thiệp).

```java
@Test
@WithMockUser(username = "stranger_user", roles = "USER")
void deleteProduct_whenCallerNotOwner_returnsUnauthorizedAction() throws Exception {
    mockMvc.perform(delete("/api/v1/products/99"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED_ACTION"));
}
```

---

## 4. Integration & Regression Tests: Kiểm Thử Tích Hợp & Chống Tái Phát Lỗi

- **Mục tiêu**: Kiểm tra sự phối hợp giữa nhiều thành phần (Controller -> Service -> Repository -> Database) trong cùng một luồng nghiệp vụ thực tế.
- **Regression Test**: Khi sửa một bug nghiệp vụ, bắt buộc viết một test case tái hiện chính xác lỗi đó trước khi sửa code, và đảm bảo test chuyển sang màu xanh sau khi sửa.
- **Cấu hình**:
  - `@SpringBootTest`
  - `@AutoConfigureMockMvc`
  - `@ActiveProfiles("test")`
  - `@Transactional`: Mặc định rollback toàn bộ thay đổi dữ liệu sau mỗi test method để bảo toàn tính độc lập của suite test.

---

## 5. Thực Tế Kiểm Thử Cơ Sở Dữ Liệu: H2 vs PostgreSQL & Flyway

### 5.1. Giới hạn cốt lõi của môi trường Test hiện tại
Trong `src/test/resources/application-test.properties`:
- Hệ quản trị: `jdbc:h2:mem:campushub-test;MODE=PostgreSQL`
- Schema DDL: `spring.jpa.hibernate.ddl-auto=create-drop`
- Flyway: `spring.flyway.enabled=false`

### 5.2. CẢNH BÁO NGUY HIỂM VỀ GIẢ ĐỊNH DATABASE
> **Test chạy thành công trên H2 KHÔNG CHỨNG MINH Flyway migrations và PostgreSQL hoạt động hoàn toàn đúng.**

- H2 ở chế độ `MODE=PostgreSQL` chỉ mô phỏng một phần cú pháp SQL; nó **KHÔNG** chạy các file migration `V1__init.sql` ... `V16__...sql` vì Flyway bị tắt trong test profile.
- H2 không hỗ trợ đầy đủ các tính năng đặc thù của PostgreSQL:
  - Kiểu dữ liệu native `JSONB`, toán tử tìm kiếm JSON.
  - Khóa bi quan (`SELECT FOR UPDATE`) có hành vi khóa khác nhau giữa H2 và PostgreSQL.
  - Các hàm thời gian và index đặc thù.
- **Chiến lược kiểm thử đúng đắn**:
  1. Kiểm thử nhanh hàng ngày (Local & CI): Sử dụng H2 test profile để verify business logic, mapping JPA và validation.
  2. Kiểm thử schema & migration trước khi release: Phải chạy Flyway migration trên cơ sở dữ liệu PostgreSQL thực tế (hoặc profile test nối PostgreSQL) để đảm bảo migration không bị lỗi cú pháp hay xung đột khóa ngoại.
  3. Tuyệt đối không sửa đổi các file Flyway đã áp dụng. Khi thay đổi schema, luôn tạo version migration mới (`V17__...`).

---

## 6. Kiểm Thử Trạng Thái Biên (Boundary & Negative Tests)

Mọi tính năng quan trọng phải có các test case kiểm tra biên:
- **Chuỗi văn bản**: Rỗng `""`, khoảng trắng `"   "`, độ dài vượt ngưỡng (ví dụ tiêu đề 201 ký tự), ký tự Unicode đặc biệt.
- **Số liệu**: Giá trị 0, số âm, số vượt trần `BigDecimal`.
- **Đồng thời (Concurrency)**: Kiểm tra 2 thread cùng cập nhật 1 sản phẩm có cùng `version` ban đầu -> Một request thành công, request thứ hai phải nhận `ErrorCode.CONCURRENT_UPDATE` (HTTP 409).
- **Vòng đời đơn hàng**: Kiểm tra không thể chuyển trạng thái đơn hàng bất hợp lệ (ví dụ: từ `CANCELLED` chuyển sang `COMPLETED`).

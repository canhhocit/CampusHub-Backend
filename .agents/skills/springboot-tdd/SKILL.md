---
name: springboot-tdd
description: Test-driven development, unit testing, integration testing, authorization verification, and regression test practices for CampusHub-Backend. Use when adding features, fixing bugs, writing tests, or verifying database and security boundaries.
metadata:
  origin: ECC
---

# Spring Boot TDD & Testing Workflow (CampusHub Quality Assurance)

Hướng dẫn kiểm thử phần mềm đa tầng cho repository CampusHub-Backend (JUnit 5, Mockito, MockMvc, SpringBootTest, AssertJ).

> Xem tài liệu chi tiết tại: [references/testing-patterns.md](references/testing-patterns.md)

---

## 1. Phân Định Các Tầng Kiểm Thử

CampusHub phân tách rõ mục đích của từng tầng test để tối ưu tốc độ và độ tin cậy:

| Tầng Kiểm Thử | Annotation Chính | Mục Đích & Phạm Vi | Tốc Độ |
| :--- | :--- | :--- | :--- |
| **Unit Test** | `@ExtendWith(MockitoExtension.class)` | Kiểm tra logic tính toán, máy trạng thái, kiểm tra điều kiện nghiệp vụ trong Service. Mock toàn bộ repository và network client. | Siêu nhanh (< 100ms) |
| **WebMvc Slice** | `@WebMvcTest(Controller.class)` hoặc MockMvc | Kiểm tra định dạng JSON, Bean Validation `@Valid`, mã HTTP status, và bảo vệ `@PreAuthorize`. | Nhanh (~ 1s) |
| **Integration Test** | `@SpringBootTest` + `@AutoConfigureMockMvc` | Kiểm tra luồng liên module hoàn chỉnh (Order -> Locker -> Payment) với Spring Context và database test. | Trung bình (~ 3-5s) |
| **Persistence Test**| `@DataJpaTest` | Kiểm tra truy vấn Spring Data JPA, derived queries, và `@Query` phức tạp. | Nhanh (~ 1-2s) |

---

## 2. Unit Tests (JUnit 5 + Mockito)

Áp dụng cấu trúc chuẩn **Arrange - Act - Assert**, mock toàn bộ dependencies, kiểm tra cả luồng thành công và ngoại lệ:

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
        product.setStatus(ProductStatus.SOLD);
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

## 3. Web Layer & Authorization Testing (MockMvc)

### Kiểm thử quyền hạn & chống IDOR bắt buộc:
Mọi endpoint chỉnh sửa tài nguyên phải kiểm tra cả 3 trường hợp:
1. Không đăng nhập (Anonymous) -> `401 Unauthorized`
2. Đăng nhập nhưng không sở hữu tài nguyên (Non-owner / Attacker) -> `403 Forbidden` hoặc `400 Bad Request` với mã `UNAUTHORIZED_ACTION`
3. Chủ sở hữu hợp pháp -> `200 OK`

> **Lưu ý tương thích Spring Boot 3.4.1**: Mã nguồn kiểm thử hiện tại (`IdentityIntegrationTest.java`) sử dụng `@MockBean` (`org.springframework.boot.test.mock.mockito.MockBean`). Spring Boot 3.4 giới thiệu thêm `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`). Cả hai đều hợp lệ trên Spring Boot 3.4.1; giữ nguyên quy ước hiện có của codebase và không tự ý thay đổi hàng loạt import kiểm thử đang chạy ổn định.

```java
@WebMvcTest(MarketplaceController.class)
class MarketplaceControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MarketplaceService marketplaceService;

    @Test
    void createProduct_whenAnonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Demo\",\"price\":10000}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "attacker_user", roles = "USER")
    void updateProduct_whenNotOwner_returnsForbiddenOrUnauthorizedAction() throws Exception {
        when(marketplaceService.updateProduct(eq(1L), any(), eq("attacker_user")))
            .thenThrow(new AppException(ErrorCode.UNAUTHORIZED_ACTION));

        mockMvc.perform(put("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hacked Title\",\"price\":50000,\"version\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED_ACTION"));
    }
}
```

---

## 4. Integration & Business Regression Testing (`@SpringBootTest`)

Sử dụng profile `test` để chạy kiểm thử tích hợp luồng nghiệp vụ:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BusinessIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @WithMockUser(username = "seller@test.com", roles = "USER")
    void fullProductCreationFlow_persistsToDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {
                    "title": "Sách Giáo Trình Giải Tích",
                    "description": "Sách còn mới 95%",
                    "price": 50000,
                    "categoryId": 1
                  }
                """))
            .andExpect(status().isCreated());

        Optional<Product> saved = productRepository.findByTitle("Sách Giáo Trình Giải Tích");
        assertThat(saved).isPresent();
        assertThat(saved.get().getPrice()).isEqualByComparingTo("50000");
    }
}
```

---

## 5. THỰC TẾ KIỂM THỬ CƠ SỞ DỮ LIỆU: H2 VS POSTGRESQL & FLYWAY

### CẢNH BÁO NGUY HIỂM VỀ GIẢ ĐỊNH DATABASE
Trong `application-test.properties`, dự án sử dụng:
- Database: H2 in-memory (`jdbc:h2:mem:campushub-test`)
- Hibernate: `ddl-auto=create-drop`
- Flyway: `spring.flyway.enabled=false`

> **Test chạy thành công trên H2 KHÔNG CHỨNG MINH Flyway migrations và PostgreSQL hoạt động hoàn toàn đúng.**

- H2 không chạy các file migration `V1`..`V16` (vì Flyway bị tắt trong test profile để tăng tốc độ kiểm thử).
- H2 không phát hiện được lỗi cú pháp đặc thù của PostgreSQL (như kiểu dữ liệu JSONB, hàm thời gian PostgreSQL, index đặc thù).
- **Quy tắc**:
  - H2 dùng để verify nhanh business logic, quan hệ JPA và validation trong lúc dev/CI.
  - Khi có migration mới (`V...__...sql`), bắt buộc phải chạy thử Flyway trên PostgreSQL thật trước khi merge mã nguồn.

---

## 6. Kiểm Thử Đồng Thời & Khóa Lạc Quan (Optimistic Locking)

Xác minh hệ thống không bị lỗi Lost Update khi 2 giao dịch cập nhật cùng một bản ghi:

```java
@Test
void updateProduct_whenVersionMismatch_throwsConcurrentUpdate() {
    Product product = new Product();
    product.setVersion(2L); // Version hiện tại trong DB là 2

    ProductUpdateRequest req = new ProductUpdateRequest();
    req.setVersion(1L); // Client gửi lên version cũ là 1

    assertThatThrownBy(() -> marketplaceService.validateVersion(product, req.getVersion()))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONCURRENT_UPDATE);
}
```

---

## 7. Các Nguyên Tắc Tránh Viết Test Vô Nghĩa (Anti-Patterns)

- **Không viết test getter/setter đơn thuần**: Chỉ test hành vi nghiệp vụ và phương thức có logic xử lý.
- **Không mock rồi assert lại chính cái vừa mock**: Phải assert kết quả đầu ra thực tế hoặc xác nhận `verify(...)` hành động có tác dụng phụ.
- **Không bỏ qua nhánh lỗi**: Mỗi hàm nghiệp vụ phải có ít nhất 1 test cho Happy Path và các test cho nhánh điều kiện biên/lỗi.
- **Viết Regression Test cho mọi bug**: Khi sửa 1 bug, viết test tái hiện lỗi trước để bảo vệ chống tái phát trong tương lai.

---

## 8. Lệnh Thực Thi Kiểm Thử

- Chạy toàn bộ test suite:
  ```powershell
  mvn test
  ```
- Chạy một class test cụ thể:
  ```powershell
  mvn test -Dtest=OrderServiceTest
  ```
- Chạy một method test đơn lẻ:
  ```powershell
  mvn test -Dtest=OrderServiceTest#createOrder_whenProductNotActive_throwsAppException
  ```
- Chạy kiểm tra toàn diện cả build và package:
  ```powershell
  mvn verify
  ```

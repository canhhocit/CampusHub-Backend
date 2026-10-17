package com.campushub.modules.payment;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.marketplace.entity.Category;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.CategoryRepository;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.entity.PaymentMethod;
import com.campushub.modules.order.entity.PaymentStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.payment.dto.PaymentCreateRequest;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User buyer;
    private User seller;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setUsername("buyer_pay");
        buyer.setEmail("buyer_pay@test.com");
        buyer.setPasswordHash("hash");
        buyer.setFullName("Buyer Pay");
        buyer.setRole(Role.STUDENT);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.save(buyer);

        seller = new User();
        seller.setUsername("seller_pay");
        seller.setEmail("seller_pay@test.com");
        seller.setPasswordHash("hash");
        seller.setFullName("Seller Pay");
        seller.setRole(Role.STUDENT);
        seller.setStatus(UserStatus.ACTIVE);
        seller = userRepository.save(seller);

        Category category = new Category();
        category.setName("Giao Trinh Pay");
        category.setSlug("giao-trinh-pay-" + System.nanoTime());
        category.setActive(true);
        category = categoryRepository.save(category);

        product = new Product();
        product.setTitle("Sach Toan Cao Cap Pay");
        product.setDescription("Sach toan");
        product.setPrice(new BigDecimal("150000"));
        product.setCategory(category);
        product.setOwner(seller);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        order = new Order();
        order.setOrderNumber("ORD-PAY-TEST-001");
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setProduct(product);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.AWAITING_DEPOSIT);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order = orderRepository.save(order);
    }

    @Test
    @WithMockUser(username = "buyer_pay")
    void testMockPaymentSuccess() throws Exception {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setPaymentMethod(PaymentMethod.MOCK);

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("MOCK"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertEquals(PaymentMethod.MOCK, updated.getPaymentMethod());
        assertNotNull(updated.getPaymentPaidAt());
    }

    @Test
    @WithMockUser(username = "buyer_pay")
    void testPayOSPaymentCreateAndStatusPolling() throws Exception {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setPaymentMethod(PaymentMethod.PAYOS);

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentMethod").value("PAYOS"))
                .andExpect(jsonPath("$.qrCode").isNotEmpty())
                .andExpect(jsonPath("$.checkoutUrl").isNotEmpty())
                .andExpect(jsonPath("$.payosOrderCode").isNumber());

        // Test status polling endpoint
        mockMvc.perform(get("/api/v1/orders/" + order.getId() + "/payment-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentMethod").value("PAYOS"))
                .andExpect(jsonPath("$.qrCode").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "buyer_pay")
    void testSimulatePayOSSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay/simulate-success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("PAYOS"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertEquals(PaymentMethod.PAYOS, updated.getPaymentMethod());
    }

    @Test
    void testPayOSWebhookPublicEndpoint() throws Exception {
        order.setPayosOrderCode(987654321L);
        orderRepository.save(order);

        java.util.Map<String, Object> webhookPayload = java.util.Map.of(
                "code", "00",
                "desc", "success",
                "data", java.util.Map.of(
                        "orderCode", 987654321L,
                        "amount", 150000L
                ),
                "signature", "dummy_sig"
        );

        // Webhook endpoint does not require auth
        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertEquals(PaymentMethod.PAYOS, updated.getPaymentMethod());
    }

    @Test
    @WithMockUser(username = "buyer_pay")
    void testPreventDoublePayment() throws Exception {
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setPaymentMethod(PaymentMethod.MOCK);

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}

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
import com.campushub.modules.payment.service.PayOSService;
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
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    private PayOSService payOSService;

    private User buyer;
    private User seller;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        buyer = userRepository.findByUsername("buyer_pay").orElseGet(() -> {
            User u = new User();
            u.setUsername("buyer_pay");
            u.setEmail("buyer_pay@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Buyer Pay");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        userRepository.findByUsername("other_user").orElseGet(() -> {
            User u = new User();
            u.setUsername("other_user");
            u.setEmail("other_user@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Other User");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        seller = userRepository.findByUsername("seller_pay").orElseGet(() -> {
            User u = new User();
            u.setUsername("seller_pay");
            u.setEmail("seller_pay@test.com");
            u.setPasswordHash("hash");
            u.setFullName("Seller Pay");
            u.setRole(Role.STUDENT);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        Category category = new Category();
        category.setName("Giao Trinh Pay " + System.nanoTime());
        category.setSlug("giao-trinh-pay-" + System.nanoTime());
        category.setActive(true);
        category = categoryRepository.save(category);

        product = new Product();
        product.setTitle("Sach Toan Cao Cap Pay " + System.nanoTime());
        product.setDescription("Sach toan");
        product.setPrice(new BigDecimal("150000"));
        product.setCategory(category);
        product.setOwner(seller);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        order = new Order();
        order.setOrderNumber("ORD-PAY-TEST-" + System.nanoTime());
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setProduct(product);
        order.setPriceSnapshot(product.getPrice());
        order.setStatus(OrderStatus.AWAITING_DEPOSIT);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order = orderRepository.save(order);
    }

    private Webhook createWebhook(long orderCode, long amount, String code) {
        WebhookData data = WebhookData.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description("Don test " + orderCode)
                .accountNumber("123456789")
                .reference("REF-" + orderCode)
                .transactionDateTime("2026-10-27 16:00:00")
                .currency("VND")
                .paymentLinkId("LINK-" + orderCode)
                .code(code)
                .desc(code.equals("00") ? "success" : "failed")
                .build();

        String signature = payOSService.createSignature(data);

        Webhook webhook = new Webhook();
        webhook.setCode(code);
        webhook.setDesc(code.equals("00") ? "success" : "failed");
        webhook.setSuccess(code.equals("00"));
        webhook.setData(data);
        webhook.setSignature(signature);
        return webhook;
    }

    // ==========================================
    // 1. VALID WEBHOOK
    // ==========================================
    @Test
    void testValidWebhook() throws Exception {
        long orderCode = 111222333L;
        order.setPayosOrderCode(orderCode);
        orderRepository.save(order);

        Webhook webhook = createWebhook(orderCode, 150000L, "00");

        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.desc").value("success"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertEquals(PaymentMethod.PAYOS, updated.getPaymentMethod());
        assertNotNull(updated.getPaymentPaidAt());
        assertEquals("REF-" + orderCode, updated.getPayosTransactionReference());
    }

    // ==========================================
    // 2. INVALID SIGNATURE
    // ==========================================
    @Test
    void testInvalidSignature() throws Exception {
        long orderCode = 222333444L;
        order.setPayosOrderCode(orderCode);
        orderRepository.save(order);

        Webhook webhook = createWebhook(orderCode, 150000L, "00");
        webhook.setSignature("bad_forged_signature_12345");

        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PAYMENT_SIGNATURE_INVALID"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.UNPAID, updated.getPaymentStatus());
        assertNull(updated.getPaymentPaidAt());
    }

    // ==========================================
    // 3. INCORRECT ORDER CODE (UNKNOWN ORDER)
    // ==========================================
    @Test
    void testIncorrectOrderCodeUnknownOrder() throws Exception {
        long unknownOrderCode = 999999999L;
        Webhook webhook = createWebhook(unknownOrderCode, 150000L, "00");

        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    // ==========================================
    // 4. INCORRECT AMOUNT
    // ==========================================
    @Test
    void testIncorrectAmount() throws Exception {
        long orderCode = 333444555L;
        order.setPayosOrderCode(orderCode);
        orderRepository.save(order);

        // Order total is 150,000 VND, but webhook carries 100,000 VND
        Webhook webhook = createWebhook(orderCode, 100000L, "00");

        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAYMENT_AMOUNT"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.UNPAID, updated.getPaymentStatus());
    }

    // ==========================================
    // 5. FAILED OR NON-SUCCESS PAYMENT EVENT
    // ==========================================
    @Test
    void testFailedPaymentEvent() throws Exception {
        long orderCode = 444555666L;
        order.setPayosOrderCode(orderCode);
        orderRepository.save(order);

        // Webhook code "01" indicates transaction failed/cancelled
        Webhook webhook = createWebhook(orderCode, 150000L, "01");

        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PAYMENT_FAILED"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.UNPAID, updated.getPaymentStatus());
    }

    // ==========================================
    // 6. DUPLICATE WEBHOOK (IDEMPOTENCY)
    // ==========================================
    @Test
    void testDuplicateWebhook() throws Exception {
        long orderCode = 555666777L;
        LocalDateTime initialPaidAt = LocalDateTime.now().minusMinutes(5);

        order.setPayosOrderCode(orderCode);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(PaymentMethod.PAYOS);
        order.setPaymentPaidAt(initialPaidAt);
        order.setPayosTransactionReference("ORIGINAL_REF");
        orderRepository.save(order);

        Webhook webhook = createWebhook(orderCode, 150000L, "00");

        // Duplicate webhook should safely return success without modifying state
        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.desc").value("success"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertEquals("ORIGINAL_REF", updated.getPayosTransactionReference());
    }

    // ==========================================
    // 7. CONCURRENT DUPLICATE NOTIFICATIONS
    // ==========================================
    @Test
    void testConcurrentDuplicateNotifications() throws Exception {
        long orderCode = 666777888L;
        order.setPayosOrderCode(orderCode);
        orderRepository.save(order);

        Webhook webhook = createWebhook(orderCode, 150000L, "00");
        String payload = objectMapper.writeValueAsString(webhook);

        int threads = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successResponses = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    mockMvc.perform(post("/api/v1/payments/payos/webhook")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.code").value("00"));
                    successResponses.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed);
        assertEquals(threads, successResponses.get());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertNotNull(updated.getPaymentPaidAt());
    }

    // ==========================================
    // 8. SIMULATION ENDPOINT PERMITTED IN TEST/LOCAL
    // ==========================================
    @Test
    @WithMockUser(username = "buyer_pay")
    void testSimulatePayOSSuccessInLocalEnvironment() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay/simulate-success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("PAYOS"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertEquals(PaymentMethod.PAYOS, updated.getPaymentMethod());
    }

    // ==========================================
    // 9. EXISTING PAYMENT BEHAVIOR & AUTHORIZATION
    // ==========================================
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
                .andExpect(jsonPath("$.paymentMethod").value("MOCK"))
                .andExpect(jsonPath("$.message").value("Thanh toán giả lập thành công. Đơn hàng đã được xác nhận thanh toán."));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertEquals(PaymentMethod.MOCK, updated.getPaymentMethod());
        assertNotNull(updated.getPaymentPaidAt());
    }

    @Test
    @WithMockUser(username = "other_user")
    void testNonBuyerCannotPayOrder() throws Exception {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setPaymentMethod(PaymentMethod.MOCK);

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED_ACTION"));
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ORDER_ALREADY_PAID"));
    }

    @Test
    @WithMockUser(username = "buyer_pay")
    void testPayOSPaymentCreateAndStatusPollingReusesLink() throws Exception {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setPaymentMethod(PaymentMethod.PAYOS);

        // First payment request creates link and caches on Order
        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentMethod").value("PAYOS"))
                .andExpect(jsonPath("$.qrCode").isNotEmpty())
                .andExpect(jsonPath("$.checkoutUrl").isNotEmpty())
                .andExpect(jsonPath("$.payosOrderCode").isNumber());

        Order saved = orderRepository.findById(order.getId()).orElseThrow();
        assertNotNull(saved.getPayosCheckoutUrl());
        assertNotNull(saved.getPayosQrCode());
        String originalCheckoutUrl = saved.getPayosCheckoutUrl();

        // Polling payment-status returns cached link without regenerating
        mockMvc.perform(get("/api/v1/orders/" + order.getId() + "/payment-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.checkoutUrl").value(originalCheckoutUrl))
                .andExpect(jsonPath("$.qrCode").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Đơn hàng chưa được thanh toán."));

        // Second payment request reuses existing link
        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value(originalCheckoutUrl));
    }

    @Test
    @WithMockUser(username = "buyer_pay")
    void testCancelledOrderCannotBePaid() throws Exception {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setPaymentMethod(PaymentMethod.PAYOS);

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ORDER_STATUS"));
    }
}

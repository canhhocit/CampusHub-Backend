package com.campushub.modules.payment.controller;

import com.campushub.modules.payment.dto.PaymentCreateRequest;
import com.campushub.modules.payment.dto.PaymentResponse;
import com.campushub.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

import java.util.Map;

@RestController
@Tag(name = "Payment", description = "Thanh toán cọc/đơn hàng (Mock & PayOS)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/orders/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Khởi tạo hoặc thực hiện thanh toán cho đơn hàng (hỗ trợ MOCK hoặc PAYOS)")
    public ResponseEntity<PaymentResponse> payOrder(
            @PathVariable Long id,
            @Valid @RequestBody PaymentCreateRequest request,
            Authentication authentication) {
        PaymentResponse response = paymentService.payOrder(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/orders/{id}/payment-status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy trạng thái thanh toán hiện tại của đơn hàng (dùng cho polling giao diện)")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable Long id,
            Authentication authentication) {
        PaymentResponse response = paymentService.getPaymentStatus(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/orders/{id}/pay/simulate-success")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mô phỏng quét mã PayOS thành công (dành cho demo & kiểm thử local không có public domain ngrok)")
    public ResponseEntity<PaymentResponse> simulatePayOSSuccess(
            @PathVariable Long id,
            Authentication authentication) {
        PaymentResponse response = paymentService.simulateWebhookSuccess(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/payments/payos/webhook")
    @Operation(summary = "Webhook tiếp nhận thông báo thanh toán thành công tự động từ PayOS")
    public ResponseEntity<Map<String, Object>> handlePayOSWebhook(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = paymentService.processPayOSWebhookPayload(payload);
        return ResponseEntity.ok(result);
    }
}

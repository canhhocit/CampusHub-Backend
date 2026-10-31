package com.campushub.modules.payment.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import com.campushub.modules.order.entity.PaymentMethod;
import com.campushub.modules.order.entity.PaymentStatus;
import com.campushub.modules.order.repository.OrderRepository;
import com.campushub.modules.payment.dto.PaymentCreateRequest;
import com.campushub.modules.payment.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final PayOSService payOSService;
    private final com.campushub.infrastructure.idempotency.IdempotencyService idempotencyService;

    public PaymentService(OrderRepository orderRepository,
                          PayOSService payOSService,
                          com.campushub.infrastructure.idempotency.IdempotencyService idempotencyService) {
        this.orderRepository = orderRepository;
        this.payOSService = payOSService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PaymentResponse payOrder(Long orderId, PaymentCreateRequest request, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED
                || order.getStatus() == OrderStatus.EXPIRED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PAID);
        }

        PaymentMethod method = request.getPaymentMethod();
        if (method == PaymentMethod.MOCK) {
            // Luồng 1: Thanh toán giả lập (Demo tức thì)
            order.setPaymentMethod(PaymentMethod.MOCK);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentPaidAt(LocalDateTime.now());
            orderRepository.save(order);

            PaymentResponse response = buildPaymentResponse(order);
            response.setSuccess(true);
            response.setMessage("Thanh toán giả lập thành công. Đơn hàng đã được xác nhận thanh toán.");
            return response;
        } else if (method == PaymentMethod.PAYOS) {
            // Luồng 2: Thanh toán VietQR qua PayOS
            // Nếu đã tạo link trước đó và chưa thanh toán, tái sử dụng link hiện tại để tránh sinh duplicate link
            if (order.getPayosOrderCode() != null && order.getPayosCheckoutUrl() != null) {
                PaymentResponse response = buildPaymentResponse(order);
                response.setSuccess(true);
                response.setCheckoutUrl(order.getPayosCheckoutUrl());
                response.setQrCode(order.getPayosQrCode());
                response.setMessage("Liên kết thanh toán VietQR qua PayOS đã sẵn sàng. Vui lòng quét mã QR hoặc mở liên kết để hoàn tất thanh toán.");
                return response;
            }

            Long orderCode = order.getPayosOrderCode();
            if (orderCode == null) {
                orderCode = payOSService.generateOrderCode(order.getId());
                order.setPayosOrderCode(orderCode);
            }
            order.setPaymentMethod(PaymentMethod.PAYOS);

            CreatePaymentLinkResponse linkResponse = payOSService.createPaymentLink(order, orderCode);
            order.setPayosPaymentLinkId(linkResponse.getPaymentLinkId());
            order.setPayosCheckoutUrl(linkResponse.getCheckoutUrl());
            order.setPayosQrCode(linkResponse.getQrCode());
            orderRepository.save(order);

            PaymentResponse response = buildPaymentResponse(order);
            response.setSuccess(true);
            response.setCheckoutUrl(linkResponse.getCheckoutUrl());
            response.setQrCode(linkResponse.getQrCode());
            response.setMessage("Tạo liên kết thanh toán VietQR qua PayOS thành công. Vui lòng quét mã QR hoặc mở liên kết để thanh toán.");
            return response;
        } else {
            throw new AppException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getBuyer() != null && order.getBuyer().getUsername().equals(username);
        boolean isSeller = order.getSeller() != null && order.getSeller().getUsername().equals(username);

        if (!isBuyer && !isSeller) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        PaymentResponse response = buildPaymentResponse(order);
        response.setSuccess(true);

        // Lấy lại URL và QR code đã lưu từ trước cho Frontend mà không gọi API tạo link mới
        if (order.getPaymentMethod() == PaymentMethod.PAYOS) {
            response.setCheckoutUrl(order.getPayosCheckoutUrl());
            response.setQrCode(order.getPayosQrCode());
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            response.setMessage("Đơn hàng đã được ghi nhận thanh toán thành công.");
        } else {
            response.setMessage("Đơn hàng chưa được thanh toán.");
        }

        return response;
    }

    @Transactional
    public Map<String, Object> processPayOSWebhook(Webhook webhook) {
        // 1. Xác thực chữ ký webhook bằng chính thức PayOS SDK
        WebhookData webhookData = payOSService.verifyWebhook(webhook);
        if (webhookData == null) {
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID, "Dữ liệu webhook PayOS không hợp lệ");
        }

        // 2. Xác thực sự kiện thanh toán thành công theo chuẩn payload PayOS
        if (!"00".equals(webhook.getCode()) || (webhookData.getCode() != null && !"00".equals(webhookData.getCode()))) {
            log.warn("PayOS webhook reported non-success payment event: webhookCode={}, dataCode={}",
                    webhook.getCode(), webhookData.getCode());
            throw new AppException(ErrorCode.PAYMENT_FAILED,
                    "Sự kiện thanh toán PayOS không thành công (code: " + webhook.getCode() + ")");
        }

        // 3. Khóa hàng theo payosOrderCode (Pessimistic Lock) để chống xung đột đồng thời và bảo đảm Idempotency
        Long orderCode = webhookData.getOrderCode();
        if (orderCode == null) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS, "Thiếu orderCode trong dữ liệu webhook PayOS");
        }

        // Idempotency lock via Redis: prevent duplicate concurrent processing of the same webhook event
        String idempotencyKey = "payos:webhook:" + orderCode;
        if (!idempotencyService.tryAcquire(idempotencyKey, java.time.Duration.ofMinutes(5))) {
            log.info("PayOS webhook for orderCode {} is already being processed or completed.", orderCode);
            return Map.of("code", "00", "desc", "success");
        }

        try {
            Order order = orderRepository.findByPayosOrderCodeWithLock(orderCode)
                    .orElseThrow(() -> {
                        log.warn("PayOS webhook orderCode không tồn tại trong hệ thống: {}", orderCode);
                        return new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng với mã PayOS: " + orderCode);
                    });

            // 4. Xử lý webhook trùng lặp an toàn (Idempotent)
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.info("PayOS webhook duplicate notification for already paid order #{} (orderCode={})", order.getId(), orderCode);
                idempotencyService.markCompleted(idempotencyKey, java.time.Duration.ofMinutes(10));
                return Map.of("code", "00", "desc", "success");
            }

            // 5. Đối soát số tiền thanh toán nhận được so với số tiền của đơn hàng
            Long receivedAmount = webhookData.getAmount();
            if (receivedAmount == null) {
                throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Thiếu số tiền trong dữ liệu thanh toán PayOS");
            }

            BigDecimal expectedAmount = order.getTotalAmount();
            if (expectedAmount.compareTo(BigDecimal.valueOf(receivedAmount)) != 0) {
                log.error("PayOS webhook amount mismatch for order #{}: expected={}, received={}",
                        order.getId(), expectedAmount, receivedAmount);
                throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                        "Số tiền thanh toán (" + receivedAmount + " VNĐ) không khớp với giá trị đơn hàng (" + expectedAmount + " VNĐ)");
            }

            // 6. Kiểm tra trạng thái đơn hàng có hợp lệ để nhận thanh toán
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED
                    || order.getStatus() == OrderStatus.EXPIRED) {
                log.error("PayOS webhook received for order #{} in invalid status: {}", order.getId(), order.getStatus());
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS, "Đơn hàng đã bị hủy hoặc hết hạn, không thể nhận thanh toán");
            }

            // 7. Cập nhật trạng thái thanh toán đơn hàng
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentMethod(PaymentMethod.PAYOS);
            order.setPaymentPaidAt(LocalDateTime.now());
            if (webhookData.getReference() != null && !webhookData.getReference().isBlank()) {
                order.setPayosTransactionReference(webhookData.getReference());
            }
            orderRepository.save(order);

            log.info("Xác nhận thanh toán PayOS thành công cho đơn #{} (orderCode={}, amount={}, ref={}) qua Webhook",
                    order.getId(), orderCode, receivedAmount, webhookData.getReference());

            idempotencyService.markCompleted(idempotencyKey, java.time.Duration.ofMinutes(10));
            return Map.of("code", "00", "desc", "success");
        } catch (Exception e) {
            idempotencyService.release(idempotencyKey);
            throw e;
        }
    }

    @Transactional
    public PaymentResponse simulateWebhookSuccess(Long orderId, String username) {
        if (!payOSService.isSimulationAllowed()) {
            throw new AppException(ErrorCode.SIMULATION_NOT_ALLOWED);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PAID);
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED
                || order.getStatus() == OrderStatus.EXPIRED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setPaymentMethod(PaymentMethod.PAYOS);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        PaymentResponse response = buildPaymentResponse(order);
        response.setSuccess(true);
        response.setMessage("Mô phỏng thanh toán PayOS thành công. Đơn hàng đã chuyển sang trạng thái đã thanh toán.");
        return response;
    }

    private PaymentResponse buildPaymentResponse(Order order) {
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setTotalAmount(order.getTotalAmount());
        response.setPayosOrderCode(order.getPayosOrderCode());
        response.setPaidAt(order.getPaymentPaidAt());
        return response;
    }
}

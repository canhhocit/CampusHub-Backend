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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final PayOSService payOSService;

    public PaymentService(OrderRepository orderRepository, PayOSService payOSService) {
        this.orderRepository = orderRepository;
        this.payOSService = payOSService;
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
            response.setMessage("Thanh toán giả lập thành công! Tiền cọc/tiền hàng đã được đưa vào trạng thái bảo chứng (Escrow).");
            return response;
        } else if (method == PaymentMethod.PAYOS) {
            // Luồng 2: Thanh toán VietQR qua PayOS
            Long orderCode = order.getPayosOrderCode();
            if (orderCode == null) {
                orderCode = payOSService.generateOrderCode(order.getId());
                order.setPayosOrderCode(orderCode);
            }
            order.setPaymentMethod(PaymentMethod.PAYOS);

            CreatePaymentLinkResponse linkResponse = payOSService.createPaymentLink(order, orderCode);
            order.setPayosPaymentLinkId(linkResponse.getPaymentLinkId());
            orderRepository.save(order);

            PaymentResponse response = buildPaymentResponse(order);
            response.setSuccess(true);
            response.setCheckoutUrl(linkResponse.getCheckoutUrl());
            response.setQrCode(linkResponse.getQrCode());
            response.setMessage("Tạo link thanh toán VietQR qua PayOS thành công. Vui lòng quét mã QR hoặc mở liên kết để thanh toán.");
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

        // Nếu đã tạo link PayOS nhưng chưa thanh toán, lấy lại QR Code tiện lợi cho Frontend
        if (order.getPaymentMethod() == PaymentMethod.PAYOS && order.getPaymentStatus() != PaymentStatus.PAID
                && order.getPayosOrderCode() != null) {
            try {
                CreatePaymentLinkResponse linkResponse = payOSService.createPaymentLink(order, order.getPayosOrderCode());
                response.setCheckoutUrl(linkResponse.getCheckoutUrl());
                response.setQrCode(linkResponse.getQrCode());
            } catch (Exception e) {
                log.warn("Không thể refresh PayOS link cho order #{}: {}", orderId, e.getMessage());
            }
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            response.setMessage("Đơn hàng đã được thanh toán và tiền đang được giữ trong tài khoản Escrow.");
        } else {
            response.setMessage("Đơn hàng chưa được thanh toán.");
        }

        return response;
    }

    @Transactional
    public Map<String, Object> processPayOSWebhookPayload(Map<String, Object> payload) {
        try {
            if (payload == null || !payload.containsKey("data")) {
                log.warn("PayOS webhook payload is null or missing data");
                return Map.of("code", "01", "desc", "Dữ liệu webhook không hợp lệ");
            }

            Object dataObj = payload.get("data");
            Long orderCode = null;
            if (dataObj instanceof Map<?, ?> dataMap) {
                Object oc = dataMap.get("orderCode");
                if (oc instanceof Number num) {
                    orderCode = num.longValue();
                } else if (oc != null) {
                    orderCode = Long.parseLong(oc.toString().trim());
                }
            }

            if (orderCode == null) {
                return Map.of("code", "01", "desc", "Thiếu orderCode trong data");
            }

            Optional<Order> orderOpt = orderRepository.findByPayosOrderCode(orderCode);
            if (orderOpt.isEmpty()) {
                log.warn("PayOS webhook orderCode không tồn tại trong hệ thống: {}", orderCode);
                return Map.of("code", "00", "desc", "Đã nhận webhook nhưng không tìm thấy đơn hàng");
            }

            Order order = orderOpt.get();
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaymentMethod(PaymentMethod.PAYOS);
                order.setPaymentPaidAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("Xác nhận thanh toán PayOS thành công cho đơn #{} (orderCode={}) qua Webhook", order.getId(), orderCode);
            }

            return Map.of("code", "00", "desc", "success");
        } catch (Exception e) {
            log.error("Lỗi khi xử lý PayOS webhook payload: {}", e.getMessage(), e);
            return Map.of("code", "01", "desc", e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse simulateWebhookSuccess(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        order.setPaymentMethod(PaymentMethod.PAYOS);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        PaymentResponse response = buildPaymentResponse(order);
        response.setSuccess(true);
        response.setMessage("Giả lập Webhook PayOS thành công! Đơn hàng đã chuyển sang trạng thái ĐÃ THANH TOÁN.");
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

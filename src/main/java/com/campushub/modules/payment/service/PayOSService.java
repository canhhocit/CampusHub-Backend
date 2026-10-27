package com.campushub.modules.payment.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;

@Service
public class PayOSService {

    private static final Logger log = LoggerFactory.getLogger(PayOSService.class);

    @Value("${app.payos.client-id:}")
    private String clientId;

    @Value("${app.payos.api-key:}")
    private String apiKey;

    @Value("${app.payos.checksum-key:}")
    private String checksumKey;

    @Value("${app.payos.return-url:http://localhost:5173/orders}")
    private String returnUrl;

    @Value("${app.payos.cancel-url:http://localhost:5173/orders}")
    private String cancelUrl;

    @Value("${app.payos.simulation-enabled:true}")
    private boolean simulationEnabled;

    private final Environment environment;
    private PayOS payOS;

    public PayOSService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (clientId != null && !clientId.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && checksumKey != null && !checksumKey.isBlank()) {
            try {
                this.payOS = new PayOS(clientId.trim(), apiKey.trim(), checksumKey.trim());
                log.info("PayOS initialized successfully with client ID: {}***", clientId.substring(0, Math.min(6, clientId.length())));
            } catch (Exception e) {
                log.error("Failed to initialize PayOS client: {}", e.getMessage(), e);
            }
        } else {
            log.warn("PayOS credentials not fully configured (PAYOS_CLIENT_ID / PAYOS_API_KEY / PAYOS_CHECKSUM_KEY). VietQR sandbox fallback will be used.");
        }
    }

    public boolean isConfigured() {
        return this.payOS != null;
    }

    public boolean isLiveMode() {
        if (environment != null && environment.acceptsProfiles(Profiles.of("test"))) {
            return false;
        }
        if (clientId != null && clientId.startsWith("test-")) {
            return false;
        }
        return isConfigured();
    }

    public boolean isSimulationAllowed() {
        if (environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return false;
        }
        return simulationEnabled;
    }

    public long generateOrderCode(Long orderId) {
        long base = (orderId != null ? orderId : 1L) % 1000000L;
        long timeSuffix = (System.currentTimeMillis() / 1000L) % 1000000L;
        return base * 1000000L + timeSuffix;
    }

    public CreatePaymentLinkResponse createPaymentLink(Order order, long orderCode) {
        if (order.getTotalAmount() == null) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Số tiền thanh toán không được để trống");
        }
        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Số tiền thanh toán tối thiểu là 1,000 VNĐ");
        }
        if (order.getTotalAmount().stripTrailingZeros().scale() > 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Số tiền thanh toán phải là số nguyên (VNĐ)");
        }

        long amount = order.getTotalAmount().longValue();
        String action = order.getOrderType() == OrderType.RENTAL ? "Thue" : "Mua";
        String desc = "Don " + order.getId() + " " + action;
        if (desc.length() > 25) {
            desc = desc.substring(0, 25);
        }

        String orderReturnUrl = returnUrl.endsWith("/") ? returnUrl + order.getId() : returnUrl + "/" + order.getId();
        String orderCancelUrl = cancelUrl.endsWith("/") ? cancelUrl + order.getId() : cancelUrl + "/" + order.getId();

        if (isLiveMode()) {
            try {
                CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                        .orderCode(orderCode)
                        .amount(amount)
                        .description(desc)
                        .returnUrl(orderReturnUrl)
                        .cancelUrl(orderCancelUrl)
                        .build();

                CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
                log.info("PayOS payment link created for order #{} (orderCode={}): {}", order.getId(), orderCode, response.getCheckoutUrl());
                return response;
            } catch (Exception e) {
                log.error("Error calling PayOS API: {}", e.getMessage(), e);
                throw new RuntimeException("Lỗi khi kết nối cổng PayOS: " + e.getMessage(), e);
            }
        } else {
            // Simulated VietQR response when credentials are not supplied yet
            log.info("Using simulated VietQR for order #{} (orderCode={})", order.getId(), orderCode);
            CreatePaymentLinkResponse mockResponse = new CreatePaymentLinkResponse();
            mockResponse.setOrderCode(orderCode);
            mockResponse.setAmount(amount);
            mockResponse.setDescription(desc);
            mockResponse.setPaymentLinkId("PAYOS-SIMULATED-" + orderCode);
            // Dynamic standard VietQR demo link
            String vietQrUrl = "https://img.vietqr.io/image/970422-0348782332-compact2.png?amount="
                    + amount + "&addInfo=" + java.net.URLEncoder.encode(desc, java.nio.charset.StandardCharsets.UTF_8);
            mockResponse.setQrCode(vietQrUrl);
            mockResponse.setCheckoutUrl(orderReturnUrl + "?simulated_payos=true&orderCode=" + orderCode);
            return mockResponse;
        }
    }

    public WebhookData verifyWebhook(Webhook webhook) {
        if (webhook == null || webhook.getData() == null || webhook.getSignature() == null || webhook.getSignature().isBlank()) {
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID, "Dữ liệu webhook thiếu thông tin bắt buộc hoặc chữ ký");
        }

        if (!isConfigured()) {
            throw new AppException(ErrorCode.PAYMENT_FAILED, "Cổng thanh toán PayOS chưa được cấu hình khóa bảo mật để xác thực");
        }

        try {
            return payOS.webhooks().verify(webhook);
        } catch (vn.payos.exception.WebhookException e) {
            log.warn("PayOS webhook verification failed: {}", e.getMessage());
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID, "Chữ ký webhook PayOS không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            log.error("PayOS webhook verification error: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID, "Xác thực chữ ký webhook PayOS thất bại: " + e.getMessage());
        }
    }

    /**
     * Generates a valid HMAC SHA256 signature for test payloads and internal signature generation.
     */
    public String createSignature(Object data) {
        if (payOS != null && checksumKey != null && !checksumKey.isBlank()) {
            try {
                return payOS.getCrypto().createSignatureFromObj(data, checksumKey.trim());
            } catch (Exception e) {
                log.error("Error creating PayOS signature: {}", e.getMessage(), e);
                return null;
            }
        }
        return null;
    }
}

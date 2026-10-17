package com.campushub.modules.payment.service;

import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

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

    private PayOS payOS;

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

    public long generateOrderCode(Long orderId) {
        long base = (orderId != null ? orderId : 1L) % 1000000L;
        long timeSuffix = (System.currentTimeMillis() / 1000L) % 1000000L;
        return base * 1000000L + timeSuffix;
    }

    public CreatePaymentLinkResponse createPaymentLink(Order order, long orderCode) {
        long amount = Math.max(1000L, order.getTotalAmount().longValue());
        String action = order.getOrderType() == OrderType.RENTAL ? "Thue" : "Mua";
        String desc = "Don " + order.getId() + " " + action;
        if (desc.length() > 25) {
            desc = desc.substring(0, 25);
        }

        String orderReturnUrl = returnUrl.endsWith("/") ? returnUrl + order.getId() : returnUrl + "/" + order.getId();
        String orderCancelUrl = cancelUrl.endsWith("/") ? cancelUrl + order.getId() : cancelUrl + "/" + order.getId();

        if (isConfigured()) {
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
        if (isConfigured()) {
            return payOS.webhooks().verify(webhook);
        } else {
            return webhook != null ? webhook.getData() : null;
        }
    }
}

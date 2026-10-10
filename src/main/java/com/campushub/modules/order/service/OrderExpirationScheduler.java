package com.campushub.modules.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);

    private final OrderService orderService;

    public OrderExpirationScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Periodically check and transition orders that exceeded pickup deadline to EXPIRED.
     * Compartments remain OCCUPIED until physically retrieved by administration.
     */
    @Scheduled(fixedRateString = "${app.order.expiration-check-interval-ms:60000}")
    public void runExpirationCheck() {
        try {
            orderService.checkAndExpireOrders();
        } catch (Exception e) {
            log.error("Error during scheduled order expiration check: {}", e.getMessage(), e);
        }
    }
}

package com.campushub.modules.order.controller;

import com.campushub.modules.order.dto.*;
import com.campushub.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management and pickup APIs")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            Authentication authentication) {
        OrderResponse response = orderService.createOrder(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/buying")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponse>> getBuyingOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 50) size = 50;
        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getBuyingOrders(authentication.getName(), pageable));
    }

    @GetMapping("/selling")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponse>> getSellingOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 50) size = 50;
        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getSellingOrders(authentication.getName(), pageable));
    }

    @GetMapping("/selling/pending-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getPendingSellingOrdersCount(Authentication authentication) {
        long count = orderService.getPendingSellingOrdersCount(authentication.getName());
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDetailResponse> getOrder(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrder(id, authentication.getName()));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> acceptOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) OrderAcceptRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.acceptOrder(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) OrderRejectRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.rejectOrder(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.cancelOrder(id, authentication.getName()));
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> depositOrder(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.depositOrder(id, authentication.getName()));
    }

    @PostMapping("/{id}/pickup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> pickupOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderPickupRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.pickupOrder(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/initiate-return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> initiateReturn(
            @PathVariable Long id,
            @Valid @RequestBody OrderReturnRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.initiateReturn(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/deposit-return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> depositReturn(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.depositReturn(id, authentication.getName()));
    }

    @PostMapping("/{id}/pickup-return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> pickupReturn(
            @PathVariable Long id,
            @Valid @RequestBody OrderPickupRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.pickupReturn(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/clear-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> clearExpiredOrder(
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.adminClearExpiredOrder(id));
    }
}

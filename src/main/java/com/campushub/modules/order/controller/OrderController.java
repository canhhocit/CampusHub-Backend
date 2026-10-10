package com.campushub.modules.order.controller;

import com.campushub.modules.order.dto.*;
import com.campushub.modules.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getBuyingOrders(authentication.getName(), pageable));
    }

    @GetMapping("/selling")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponse>> getSellingOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 50) size = 50;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getSellingOrders(authentication.getName(), pageable));
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
            @RequestBody(required = false) OrderAcceptRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.acceptOrder(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable Long id,
            @RequestBody(required = false) OrderRejectRequest request,
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
}

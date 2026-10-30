package com.campushub.modules.dispute.controller;

import com.campushub.modules.dispute.dto.DisputeCreateRequest;
import com.campushub.modules.dispute.dto.DisputeResponse;
import com.campushub.modules.dispute.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/disputes")
@Tag(name = "Disputes", description = "Khiếu nại và tranh chấp đơn hàng")
@PreAuthorize("isAuthenticated()")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    @Operation(summary = "Người mua gửi khiếu nại / báo cáo sự cố cho đơn hàng")
    public ResponseEntity<DisputeResponse> createDispute(
            @PathVariable Long orderId,
            @Valid @RequestBody DisputeCreateRequest request,
            Authentication authentication
    ) {
        DisputeResponse response = disputeService.createDispute(orderId, request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách các khiếu nại của đơn hàng")
    public ResponseEntity<List<DisputeResponse>> getDisputes(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(disputeService.getDisputesByOrder(orderId, authentication.getName()));
    }
}


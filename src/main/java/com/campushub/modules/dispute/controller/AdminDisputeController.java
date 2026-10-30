package com.campushub.modules.dispute.controller;

import com.campushub.modules.dispute.dto.DisputeResolveRequest;
import com.campushub.modules.dispute.dto.DisputeResponse;
import com.campushub.modules.dispute.enums.DisputeStatus;
import com.campushub.modules.dispute.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/disputes")
@Tag(name = "Admin Disputes", description = "Quản lý và giải quyết khiếu nại (Admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {

    private final DisputeService disputeService;

    public AdminDisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách khiếu nại của hệ thống (Admin)")
    public ResponseEntity<Page<DisputeResponse>> getDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(disputeService.getAdminDisputes(status, pageable));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Giải quyết khiếu nại: Hoàn tiền người mua hoặc Giữ thanh toán cho người bán")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeResolveRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, request, authentication.getName()));
    }
}


package com.campushub.modules.wallet.controller;

import com.campushub.modules.wallet.dto.AdminPayoutActionRequest;
import com.campushub.modules.wallet.dto.PayoutRequestResponse;
import com.campushub.modules.wallet.enums.PayoutStatus;
import com.campushub.modules.wallet.service.PayoutAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/payouts")
@Tag(name = "Admin Payouts", description = "Quản lý và duyệt yêu cầu rút tiền cho Admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPayoutController {

    private final PayoutAdminService payoutAdminService;

    public AdminPayoutController(PayoutAdminService payoutAdminService) {
        this.payoutAdminService = payoutAdminService;
    }

    @GetMapping
    @Operation(summary = "Danh sách yêu cầu rút tiền (Admin)")
    public ResponseEntity<Page<PayoutRequestResponse>> getPayoutRequests(
            @RequestParam(required = false) PayoutStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(payoutAdminService.getPayoutRequests(status, pageable));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Duyệt yêu cầu rút tiền (Admin xác nhận đã chuyển khoản)")
    public ResponseEntity<PayoutRequestResponse> approvePayout(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody(required = false) AdminPayoutActionRequest request
    ) {
        return ResponseEntity.ok(payoutAdminService.approvePayout(id, authentication.getName(), request));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Từ chối yêu cầu rút tiền (hoàn tiền lại vào ví người dùng)")
    public ResponseEntity<PayoutRequestResponse> rejectPayout(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody(required = false) AdminPayoutActionRequest request
    ) {
        return ResponseEntity.ok(payoutAdminService.rejectPayout(id, authentication.getName(), request));
    }
}

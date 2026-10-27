package com.campushub.modules.wallet.controller;

import com.campushub.modules.wallet.dto.*;
import com.campushub.modules.wallet.service.WalletService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "Wallet", description = "Quản lý số dư ví, tài khoản ngân hàng và rút tiền")
@PreAuthorize("isAuthenticated()")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    @Operation(summary = "Xem số dư ví của tài khoản hiện tại")
    public ResponseEntity<WalletResponse> getMyWallet(Authentication authentication) {
        return ResponseEntity.ok(walletService.getWallet(authentication.getName()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Xem lịch sử biến động số dư ví (phân trang)")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(walletService.getTransactions(authentication.getName(), pageable));
    }

    @GetMapping("/bank-accounts")
    @Operation(summary = "Danh sách tài khoản ngân hàng đã lưu")
    public ResponseEntity<List<UserBankAccountResponse>> getBankAccounts(Authentication authentication) {
        return ResponseEntity.ok(walletService.getBankAccounts(authentication.getName()));
    }

    @PostMapping("/bank-accounts")
    @Operation(summary = "Thêm hoặc cập nhật tài khoản ngân hàng")
    public ResponseEntity<UserBankAccountResponse> saveBankAccount(
            Authentication authentication,
            @Valid @RequestBody UserBankAccountRequest request
    ) {
        return ResponseEntity.ok(walletService.saveBankAccount(authentication.getName(), request));
    }

    @DeleteMapping("/bank-accounts/{id}")
    @Operation(summary = "Xóa tài khoản ngân hàng")
    public ResponseEntity<Void> deleteBankAccount(
            Authentication authentication,
            @PathVariable Long id
    ) {
        walletService.deleteBankAccount(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Gửi yêu cầu rút tiền về tài khoản ngân hàng")
    public ResponseEntity<PayoutRequestResponse> requestWithdrawal(
            Authentication authentication,
            @Valid @RequestBody WithdrawalRequestDto request
    ) {
        return ResponseEntity.ok(walletService.requestWithdrawal(authentication.getName(), request));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Danh sách yêu cầu rút tiền của tài khoản")
    public ResponseEntity<Page<PayoutRequestResponse>> getMyWithdrawals(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(walletService.getMyPayoutRequests(authentication.getName(), pageable));
    }
}

package com.campushub.modules.wallet.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.PaymentStatus;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.campushub.modules.wallet.dto.*;
import com.campushub.modules.wallet.entity.PayoutRequest;
import com.campushub.modules.wallet.entity.UserBankAccount;
import com.campushub.modules.wallet.entity.Wallet;
import com.campushub.modules.wallet.entity.WalletTransaction;
import com.campushub.modules.wallet.enums.PayoutStatus;
import com.campushub.modules.wallet.enums.TransactionType;
import com.campushub.modules.wallet.repository.PayoutRequestRepository;
import com.campushub.modules.wallet.repository.UserBankAccountRepository;
import com.campushub.modules.wallet.repository.WalletRepository;
import com.campushub.modules.wallet.repository.WalletTransactionRepository;
import com.campushub.modules.admin.service.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10000.00");

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserBankAccountRepository userBankAccountRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final UserRepository userRepository;
    private final SystemSettingService systemSettingService;
    private final com.campushub.infrastructure.idempotency.IdempotencyService idempotencyService;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            UserBankAccountRepository userBankAccountRepository,
            PayoutRequestRepository payoutRequestRepository,
            UserRepository userRepository,
            SystemSettingService systemSettingService,
            com.campushub.infrastructure.idempotency.IdempotencyService idempotencyService
    ) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.userBankAccountRepository = userBankAccountRepository;
        this.payoutRequestRepository = payoutRequestRepository;
        this.userRepository = userRepository;
        this.systemSettingService = systemSettingService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional(readOnly = true)
    public WalletConfigResponse getWalletConfig() {
        return new WalletConfigResponse(systemSettingService.getMinWithdrawalAmount());
    }

    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            Wallet w = new Wallet(user);
            return walletRepository.save(w);
        });
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> new Wallet(user));
        return new WalletResponse(user.getId(), user.getUsername(), wallet.getBalance(), wallet.getFrozenBalance());
    }

    @Transactional
    public void creditOrderPayout(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            log.warn("Order #{} is not PAID (status: {}), skipping payout credit", order.getId(), order.getPaymentStatus());
            return;
        }

        BigDecimal payout = order.getSellerPayout();
        if (payout == null || payout.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Order #{} has no seller payout to credit", order.getId());
            return;
        }

        if (walletTransactionRepository.existsByOrderIdAndType(order.getId(), TransactionType.ORDER_PAYOUT)) {
            log.warn("Order payout already credited for order #{}", order.getId());
            return;
        }

        Long sellerId = order.getSeller().getId();
        getOrCreateWallet(sellerId);
        Wallet wallet = walletRepository.findByUserIdWithLock(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(payout);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setOrderId(order.getId());
        tx.setType(TransactionType.ORDER_PAYOUT);
        tx.setAmount(payout);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Doanh thu đơn hàng " + order.getOrderNumber());
        walletTransactionRepository.save(tx);

        log.info("Credited seller payout {} VND for order #{} to user #{} (new balance: {})",
                payout, order.getId(), sellerId, after);
    }

    @Transactional
    public void refundRentalDeposit(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            log.warn("Order #{} is not PAID (status: {}), skipping deposit refund", order.getId(), order.getPaymentStatus());
            return;
        }

        BigDecimal deposit = order.getDepositAmount();
        if (deposit == null || deposit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (walletTransactionRepository.existsByOrderIdAndType(order.getId(), TransactionType.DEPOSIT_REFUND)) {
            log.warn("Rental deposit refund already credited for order #{}", order.getId());
            return;
        }

        Long buyerId = order.getBuyer().getId();
        getOrCreateWallet(buyerId);
        Wallet wallet = walletRepository.findByUserIdWithLock(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(deposit);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setOrderId(order.getId());
        tx.setType(TransactionType.DEPOSIT_REFUND);
        tx.setAmount(deposit);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Hoàn cọc đơn thuê " + order.getOrderNumber());
        walletTransactionRepository.save(tx);

        log.info("Refunded rental deposit {} VND for order #{} to user #{} (new balance: {})",
                deposit, order.getId(), buyerId, after);
    }

    @Transactional
    public void refundOrderPayment(Order order, String reason) {
        if (order == null || order.getId() == null) {
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            log.info("Order #{} is not PAID (status: {}), skipping cancellation refund", order.getId(), order.getPaymentStatus());
            return;
        }

        BigDecimal total = (order.getPriceSnapshot() != null ? order.getPriceSnapshot() : BigDecimal.ZERO)
                .add(order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (walletTransactionRepository.existsByOrderIdAndType(order.getId(), TransactionType.ORDER_REFUND)) {
            log.warn("Order refund already processed for order #{}", order.getId());
            return;
        }

        Long buyerId = order.getBuyer().getId();
        getOrCreateWallet(buyerId);
        Wallet wallet = walletRepository.findByUserIdWithLock(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(total);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setOrderId(order.getId());
        tx.setType(TransactionType.ORDER_REFUND);
        tx.setAmount(total);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription((reason != null && !reason.isBlank() ? reason : "Hoàn tiền đơn hàng") + " " + order.getOrderNumber());
        walletTransactionRepository.save(tx);

        log.info("Refunded order payment {} VND for order #{} to buyer #{} (new balance: {})",
                total, order.getId(), buyerId, after);
    }

    @Transactional
    public PayoutRequestResponse requestWithdrawal(String username, WithdrawalRequestDto request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Idempotency check: prevent duplicate concurrent withdrawal submission (double-click protection)
        String idempotencyKey = "withdraw:user:" + user.getId();
        idempotencyService.acquireOrThrow(idempotencyKey, java.time.Duration.ofSeconds(10));

        try {
            return doRequestWithdrawal(user, username, request);
        } finally {
            idempotencyService.release(idempotencyKey);
        }
    }

    private PayoutRequestResponse doRequestWithdrawal(User user, String username, WithdrawalRequestDto request) {
        BigDecimal minWithdrawal = systemSettingService.getMinWithdrawalAmount();
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(minWithdrawal) < 0) {
            throw new AppException(ErrorCode.INVALID_WITHDRAWAL_AMOUNT,
                    String.format("Số tiền rút tối thiểu là %,d VNĐ", minWithdrawal.longValue()));
        }

        UserBankAccount bankAccount;
        if (request.getBankAccountId() != null) {
            bankAccount = userBankAccountRepository.findByIdAndUserId(request.getBankAccountId(), user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
        } else {
            bankAccount = userBankAccountRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
        }

        getOrCreateWallet(user.getId());
        Wallet wallet = walletRepository.findByUserIdWithLock(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_WALLET_BALANCE);
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);
        wallet.setBalance(after);
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));
        walletRepository.save(wallet);

        PayoutRequest payout = new PayoutRequest();
        payout.setUser(user);
        payout.setAmount(amount);
        payout.setBankCode(bankAccount.getBankCode());
        payout.setAccountNumber(bankAccount.getAccountNumber());
        payout.setAccountName(bankAccount.getAccountName());
        payout.setStatus(PayoutStatus.PENDING);
        payout = payoutRequestRepository.save(payout);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setPayoutRequestId(payout.getId());
        tx.setType(TransactionType.WITHDRAWAL);
        tx.setAmount(amount.negate());
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Rút tiền về " + bankAccount.getBankCode() + " - " + bankAccount.getAccountNumber());
        walletTransactionRepository.save(tx);

        log.info("User {} requested withdrawal of {} VND (payoutRequestId={})",
                username, amount, payout.getId());

        return mapToPayoutResponse(payout);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactions(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> new Wallet(user));

        if (wallet.getId() == null) {
            return Page.empty(pageable);
        }

        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public Page<PayoutRequestResponse> getMyPayoutRequests(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return payoutRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToPayoutResponse);
    }

    @Transactional(readOnly = true)
    public List<UserBankAccountResponse> getBankAccounts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userBankAccountRepository.findByUserId(user.getId()).stream()
                .map(this::mapToBankAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserBankAccountResponse saveBankAccount(String username, UserBankAccountRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<UserBankAccount> existing = userBankAccountRepository.findByUserId(user.getId());
        boolean shouldBeDefault = request.isDefault() || existing.isEmpty();

        if (shouldBeDefault) {
            for (UserBankAccount acc : existing) {
                if (acc.isDefault()) {
                    acc.setDefault(false);
                    userBankAccountRepository.save(acc);
                }
            }
        }

        UserBankAccount account = new UserBankAccount();
        account.setUser(user);
        account.setBankCode(request.getBankCode().trim().toUpperCase());
        account.setBankName(request.getBankName());
        account.setAccountNumber(request.getAccountNumber().trim());
        account.setAccountName(request.getAccountName().trim().toUpperCase());
        account.setDefault(shouldBeDefault);

        account = userBankAccountRepository.save(account);
        return mapToBankAccountResponse(account);
    }

    @Transactional
    public void deleteBankAccount(String username, Long accountId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserBankAccount account = userBankAccountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
        userBankAccountRepository.delete(account);
    }

    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction tx) {
        WalletTransactionResponse res = new WalletTransactionResponse();
        res.setId(tx.getId());
        res.setOrderId(tx.getOrderId());
        res.setPayoutRequestId(tx.getPayoutRequestId());
        res.setType(tx.getType());
        res.setAmount(tx.getAmount());
        res.setBalanceBefore(tx.getBalanceBefore());
        res.setBalanceAfter(tx.getBalanceAfter());
        res.setDescription(tx.getDescription());
        res.setCreatedAt(tx.getCreatedAt());
        return res;
    }

    private UserBankAccountResponse mapToBankAccountResponse(UserBankAccount acc) {
        UserBankAccountResponse res = new UserBankAccountResponse();
        res.setId(acc.getId());
        res.setBankCode(acc.getBankCode());
        res.setBankName(acc.getBankName());
        res.setAccountNumber(acc.getAccountNumber());
        res.setAccountName(acc.getAccountName());
        res.setDefault(acc.isDefault());
        res.setCreatedAt(acc.getCreatedAt());
        return res;
    }

    public PayoutRequestResponse mapToPayoutResponse(PayoutRequest p) {
        PayoutRequestResponse res = new PayoutRequestResponse();
        res.setId(p.getId());
        res.setUserId(p.getUser().getId());
        res.setUsername(p.getUser().getUsername());
        res.setFullName(p.getUser().getFullName());
        res.setAmount(p.getAmount());
        res.setBankCode(p.getBankCode());
        res.setAccountNumber(p.getAccountNumber());
        res.setAccountName(p.getAccountName());
        res.setStatus(p.getStatus());
        res.setReferenceCode(p.getReferenceCode());
        res.setAdminNote(p.getAdminNote());
        if (p.getProcessedBy() != null) {
            res.setProcessedByName(p.getProcessedBy().getFullName());
        }
        res.setProcessedAt(p.getProcessedAt());
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }
}

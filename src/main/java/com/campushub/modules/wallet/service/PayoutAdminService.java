package com.campushub.modules.wallet.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.campushub.modules.wallet.dto.AdminPayoutActionRequest;
import com.campushub.modules.wallet.dto.PayoutRequestResponse;
import com.campushub.modules.wallet.entity.PayoutRequest;
import com.campushub.modules.wallet.entity.Wallet;
import com.campushub.modules.wallet.entity.WalletTransaction;
import com.campushub.modules.wallet.enums.PayoutStatus;
import com.campushub.modules.wallet.enums.TransactionType;
import com.campushub.modules.wallet.repository.PayoutRequestRepository;
import com.campushub.modules.wallet.repository.WalletRepository;
import com.campushub.modules.wallet.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PayoutAdminService {

    private static final Logger log = LoggerFactory.getLogger(PayoutAdminService.class);

    private final PayoutRequestRepository payoutRequestRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    public PayoutAdminService(
            PayoutRequestRepository payoutRequestRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            UserRepository userRepository,
            WalletService walletService
    ) {
        this.payoutRequestRepository = payoutRequestRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public Page<PayoutRequestResponse> getPayoutRequests(PayoutStatus status, Pageable pageable) {
        if (status != null) {
            return payoutRequestRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
                    .map(walletService::mapToPayoutResponse);
        }
        return payoutRequestRepository.findAll(pageable)
                .map(walletService::mapToPayoutResponse);
    }

    @Transactional
    public PayoutRequestResponse approvePayout(Long payoutRequestId, String adminUsername, AdminPayoutActionRequest request) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PayoutRequest payout = payoutRequestRepository.findByIdWithLock(payoutRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYOUT_REQUEST_NOT_FOUND));

        if (payout.getStatus() == PayoutStatus.COMPLETED || payout.getStatus() == PayoutStatus.REJECTED) {
            throw new AppException(ErrorCode.PAYOUT_ALREADY_PROCESSED);
        }

        Wallet wallet = walletRepository.findByUserIdWithLock(payout.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal currentFrozen = wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;
        BigDecimal newFrozen = currentFrozen.subtract(payout.getAmount());
        if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
            newFrozen = BigDecimal.ZERO;
        }
        wallet.setFrozenBalance(newFrozen);
        walletRepository.save(wallet);

        payout.setStatus(PayoutStatus.COMPLETED);
        if (request != null && request.getReferenceCode() != null && !request.getReferenceCode().isBlank()) {
            payout.setReferenceCode(request.getReferenceCode().trim());
        }
        if (request != null && request.getNote() != null) {
            payout.setAdminNote(request.getNote().trim());
        }
        payout.setProcessedBy(admin);
        payout.setProcessedAt(LocalDateTime.now());
        payout = payoutRequestRepository.save(payout);

        log.info("Admin {} approved payout request #{} (amount={} VND, ref={})",
                adminUsername, payout.getId(), payout.getAmount(), payout.getReferenceCode());

        return walletService.mapToPayoutResponse(payout);
    }

    @Transactional
    public PayoutRequestResponse rejectPayout(Long payoutRequestId, String adminUsername, AdminPayoutActionRequest request) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PayoutRequest payout = payoutRequestRepository.findByIdWithLock(payoutRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYOUT_REQUEST_NOT_FOUND));

        if (payout.getStatus() == PayoutStatus.COMPLETED || payout.getStatus() == PayoutStatus.REJECTED) {
            throw new AppException(ErrorCode.PAYOUT_ALREADY_PROCESSED);
        }

        Wallet wallet = walletRepository.findByUserIdWithLock(payout.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal before = wallet.getBalance();
        BigDecimal currentFrozen = wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;
        BigDecimal newFrozen = currentFrozen.subtract(payout.getAmount());
        if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
            newFrozen = BigDecimal.ZERO;
        }
        wallet.setFrozenBalance(newFrozen);

        BigDecimal after = before.add(payout.getAmount());
        wallet.setBalance(after);
        walletRepository.save(wallet);

        payout.setStatus(PayoutStatus.REJECTED);
        String note = (request != null && request.getNote() != null) ? request.getNote().trim() : "Từ chối yêu cầu rút tiền";
        payout.setAdminNote(note);
        payout.setProcessedBy(admin);
        payout.setProcessedAt(LocalDateTime.now());
        payout = payoutRequestRepository.save(payout);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setPayoutRequestId(payout.getId());
        tx.setType(TransactionType.WITHDRAWAL_REFUND);
        tx.setAmount(payout.getAmount());
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Hoàn tiền yêu cầu rút tiền bị từ chối: " + note);
        walletTransactionRepository.save(tx);

        log.info("Admin {} rejected payout request #{} (amount={} VND, reason={})",
                adminUsername, payout.getId(), payout.getAmount(), note);

        return walletService.mapToPayoutResponse(payout);
    }
}

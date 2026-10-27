package com.campushub.modules.wallet.repository;

import com.campushub.modules.wallet.entity.WalletTransaction;
import com.campushub.modules.wallet.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    boolean existsByOrderIdAndType(Long orderId, TransactionType type);
}

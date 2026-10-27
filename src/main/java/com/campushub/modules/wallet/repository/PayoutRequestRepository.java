package com.campushub.modules.wallet.repository;

import com.campushub.modules.wallet.entity.PayoutRequest;
import com.campushub.modules.wallet.enums.PayoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {

    Page<PayoutRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PayoutRequest> findByStatusOrderByCreatedAtAsc(PayoutStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PayoutRequest p WHERE p.id = :id")
    Optional<PayoutRequest> findByIdWithLock(@Param("id") Long id);
}

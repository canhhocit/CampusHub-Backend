package com.campushub.modules.dispute.repository;

import com.campushub.modules.dispute.entity.OrderDispute;
import com.campushub.modules.dispute.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDisputeRepository extends JpaRepository<OrderDispute, Long> {

    List<OrderDispute> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrderIdAndStatus(Long orderId, DisputeStatus status);

    Page<OrderDispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status, Pageable pageable);

    Page<OrderDispute> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(DisputeStatus status);
}


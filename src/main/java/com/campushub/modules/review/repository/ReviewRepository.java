package com.campushub.modules.review.repository;

import com.campushub.modules.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    Page<Review> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.seller.id = :sellerId")
    Double getAverageRatingBySellerId(@Param("sellerId") Long sellerId);

    long countBySellerId(Long sellerId);
}


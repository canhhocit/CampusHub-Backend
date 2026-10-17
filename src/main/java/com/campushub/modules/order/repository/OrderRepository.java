package com.campushub.modules.order.repository;

import com.campushub.modules.order.entity.Order;
import com.campushub.modules.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.product.id = :id AND " +
            "(o.status IN :statuses OR (o.status = com.campushub.modules.order.entity.OrderStatus.EXPIRED AND o.clearedAt IS NULL))")
    boolean hasProductReservation(@Param("id") Long id, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.compartment.id = :id AND " +
            "(o.status IN :statuses OR (o.status = com.campushub.modules.order.entity.OrderStatus.EXPIRED AND o.clearedAt IS NULL))")
    boolean hasCompartmentReservation(@Param("id") Long id, @Param("statuses") Collection<OrderStatus> statuses);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByPayosOrderCode(Long payosOrderCode);

    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.product.id = :productId AND o.status IN :statuses")
    boolean existsByProductIdAndStatusIn(@Param("productId") Long productId, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.product.id = :productId AND o.status IN :statuses")
    Optional<Order> findFirstByProductIdAndStatusIn(@Param("productId") Long productId, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.compartment.id = :compartmentId AND o.status IN :statuses")
    boolean existsByCompartmentIdAndStatusIn(@Param("compartmentId") Long compartmentId, @Param("statuses") Collection<OrderStatus> statuses);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.pickupDeadline < :now")
    java.util.List<Order> findExpiredOrders(@Param("status") OrderStatus status, @Param("now") java.time.LocalDateTime now);

    // Admin dashboard queries
    long countByStatus(OrderStatus status);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByStatusAndPickedUpAtAfter(OrderStatus status, LocalDateTime dateTime);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.buyer.id = :buyerId OR o.seller.id = :sellerId")
    long countByBuyerIdOrSellerId(@Param("buyerId") Long buyerId, @Param("sellerId") Long sellerId);
}

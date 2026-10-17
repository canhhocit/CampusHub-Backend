package com.campushub.modules.marketplace.repository;

import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "owner"})
    @Query("SELECT p FROM Product p WHERE p.status = :status AND (:categoryId IS NULL OR p.category.id = :categoryId) AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CAST(:search AS string)) OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CAST(:search AS string))))")
    Page<Product> findAllPublicProducts(@Param("status") ProductStatus status, 
                                        @Param("categoryId") Long categoryId, 
                                        @Param("search") String search, 
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"category", "owner"})
    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAllByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "owner"})
    @Query("SELECT p FROM Product p WHERE p.owner.id = :ownerId AND (:status IS NULL OR p.status = :status)")
    Page<Product> findMyProducts(@Param("ownerId") Long ownerId, @Param("status") ProductStatus status, Pageable pageable);
    
    @EntityGraph(attributePaths = {"category", "owner"})
    Optional<Product> findById(Long id);

    // Admin dashboard queries
    long countByStatus(ProductStatus status);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByOwnerId(Long ownerId);
}

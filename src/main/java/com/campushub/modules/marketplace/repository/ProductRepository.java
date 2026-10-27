package com.campushub.modules.marketplace.repository;

import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "owner", "locker"})
    @Query("SELECT p FROM Product p WHERE p.status = :status " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CAST(:search AS string)) OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CAST(:search AS string)))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:listingType IS NULL OR p.listingType = :listingType) " +
           "AND (:lockerId IS NULL OR (p.locker IS NOT NULL AND p.locker.id = :lockerId))")
    Page<Product> findAllPublicProducts(@Param("status") ProductStatus status, 
                                        @Param("categoryId") Long categoryId, 
                                        @Param("search") String search, 
                                        @Param("minPrice") java.math.BigDecimal minPrice,
                                        @Param("maxPrice") java.math.BigDecimal maxPrice,
                                        @Param("listingType") com.campushub.modules.marketplace.entity.ListingType listingType,
                                        @Param("lockerId") Long lockerId,
                                        Pageable pageable);

    default Page<Product> findAllPublicProducts(ProductStatus status, 
                                                Long categoryId, 
                                                String search, 
                                                Pageable pageable) {
        return findAllPublicProducts(status, categoryId, search, null, null, null, null, pageable);
    }

    @EntityGraph(attributePaths = {"category", "owner", "locker"})
    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAllByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "owner", "locker"})
    @Query("SELECT p FROM Product p WHERE p.owner.id = :ownerId AND (:status IS NULL OR p.status = :status)")
    Page<Product> findMyProducts(@Param("ownerId") Long ownerId, @Param("status") ProductStatus status, Pageable pageable);
    
    @EntityGraph(attributePaths = {"category", "owner", "locker"})
    Optional<Product> findById(Long id);

    // Admin dashboard queries
    long countByStatus(ProductStatus status);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByOwnerId(Long ownerId);
}

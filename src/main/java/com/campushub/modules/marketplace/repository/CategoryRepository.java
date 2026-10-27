package com.campushub.modules.marketplace.repository;

import com.campushub.modules.marketplace.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);
    List<Category> findByIsActiveTrueOrderByNameAsc();
    Optional<Category> findByIdAndIsActiveTrue(Long id);
    boolean existsBySlug(String slug);
}

package com.campushub.modules.marketplace.repository;

import com.campushub.modules.marketplace.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsActiveTrueOrderByNameAsc();
    Optional<Category> findByIdAndIsActiveTrue(Long id);
    boolean existsBySlug(String slug);
}

package com.campushub.modules.locker.repository;

import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LockerRepository extends JpaRepository<Locker, Long> {

    boolean existsByCode(String code);

    Optional<Locker> findByCode(String code);

    @Query("SELECT l FROM Locker l WHERE " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:search IS NULL OR LOWER(l.code) LIKE LOWER(:search) OR LOWER(l.name) LIKE LOWER(:search) OR LOWER(l.location) LIKE LOWER(:search))")
    Page<Locker> findAllWithFilters(@Param("status") LockerStatus status,
                                    @Param("search") String search,
                                    Pageable pageable);
}

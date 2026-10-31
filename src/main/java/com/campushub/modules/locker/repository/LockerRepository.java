package com.campushub.modules.locker.repository;

import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LockerRepository extends JpaRepository<Locker, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Locker l WHERE l.id = :id")
    Optional<Locker> findByIdWithLock(@Param("id") Long id);

    boolean existsByCode(String code);

    Optional<Locker> findByCode(String code);

    java.util.List<Locker> findByStatus(LockerStatus status);

    @Query("SELECT l FROM Locker l WHERE " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(CAST(:search AS string) IS NULL OR " +
           "LOWER(l.code) LIKE CAST(:search AS string) OR " +
           "LOWER(l.name) LIKE CAST(:search AS string) OR " +
           "LOWER(l.location) LIKE CAST(:search AS string))")
    Page<Locker> findAllWithFilters(@Param("status") LockerStatus status,
                                    @Param("search") String search,
                                    Pageable pageable);

    // Admin dashboard queries
    long countByStatus(LockerStatus status);
}

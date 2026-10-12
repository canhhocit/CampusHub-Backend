package com.campushub.modules.locker.repository;

import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompartmentRepository extends JpaRepository<Compartment, Long> {

    boolean existsByLockerIdAndCode(Long lockerId, String code);

    List<Compartment> findByLockerIdOrderByCodeAsc(Long lockerId);

    Optional<Compartment> findByIdAndLockerId(Long id, Long lockerId);

    long countByLockerId(Long lockerId);

    long countByLockerIdAndStatus(Long lockerId, CompartmentStatus status);

    Optional<Compartment> findFirstByLockerIdAndStatus(Long lockerId, CompartmentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Compartment c WHERE c.locker.status = com.campushub.modules.locker.entity.LockerStatus.ACTIVE AND c.status = com.campushub.modules.locker.entity.CompartmentStatus.AVAILABLE ORDER BY c.locker.id ASC, c.code ASC")
    List<Compartment> findAvailableInActiveLockers();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Compartment c WHERE c.id = :id")
    Optional<Compartment> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);

    // Admin dashboard queries
    long countByStatus(CompartmentStatus status);
}

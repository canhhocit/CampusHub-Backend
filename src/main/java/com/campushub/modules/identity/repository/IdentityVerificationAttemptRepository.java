package com.campushub.modules.identity.repository;

import com.campushub.modules.identity.entity.IdentityVerificationAttempt;
import com.campushub.modules.identity.enums.VerificationAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdentityVerificationAttemptRepository extends JpaRepository<IdentityVerificationAttempt, Long> {

    Optional<IdentityVerificationAttempt> findByAttemptId(String attemptId);

    @Query("SELECT a FROM IdentityVerificationAttempt a WHERE a.attemptId = :attemptId AND a.user.id = :userId")
    Optional<IdentityVerificationAttempt> findByAttemptIdAndUserId(@Param("attemptId") String attemptId, @Param("userId") Long userId);

    @Query("SELECT a FROM IdentityVerificationAttempt a WHERE a.user.id = :userId AND a.status IN :statuses AND a.expiresAt > :now")
    List<IdentityVerificationAttempt> findActiveAttempts(
            @Param("userId") Long userId,
            @Param("statuses") Collection<VerificationAttemptStatus> statuses,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT COUNT(a) FROM IdentityVerificationAttempt a WHERE a.user.id = :userId AND a.createdAt >= :since")
    long countAttemptsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM IdentityVerificationAttempt a WHERE a.citizenId = :citizenId AND a.status IN :statuses")
    List<IdentityVerificationAttempt> findByCitizenIdAndStatusIn(
            @Param("citizenId") String citizenId,
            @Param("statuses") Collection<VerificationAttemptStatus> statuses
    );

    Optional<IdentityVerificationAttempt> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}

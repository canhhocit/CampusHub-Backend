package com.campushub.modules.audit.repository;

import com.campushub.modules.audit.entity.AuditAction;
import com.campushub.modules.audit.entity.AuditEntityType;
import com.campushub.modules.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(AuditEntityType entityType, Long entityId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.success = false AND a.createdAt >= :since")
    long countFailedByActionSince(@Param("action") AuditAction action, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.success = false AND a.createdAt >= :since")
    long countFailedSince(@Param("since") LocalDateTime since);

    // Cleanup old logs (optional, for maintenance)
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Recent failed login attempts for security monitoring
    @Query("SELECT a FROM AuditLog a WHERE " +
            "a.action = 'USER_LOGIN' AND " +
            "a.success = false AND " +
            "a.createdAt >= :since " +
            "ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentFailedLogins(@Param("since") LocalDateTime since, Pageable pageable);
}

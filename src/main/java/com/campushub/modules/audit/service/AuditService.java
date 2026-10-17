package com.campushub.modules.audit.service;

import com.campushub.modules.audit.dto.AuditLogResponse;
import com.campushub.modules.audit.entity.AuditAction;
import com.campushub.modules.audit.entity.AuditEntityType;
import com.campushub.modules.audit.entity.AuditLog;
import com.campushub.modules.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log audit entry while caller security/request context is available
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void log(AuditAction action, AuditEntityType entityType, Long entityId, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDetails(details);
            log.setSuccess(true);

            enrichWithContext(log);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Log audit entry with explicit success status
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void log(AuditAction action, AuditEntityType entityType, Long entityId, 
                    String details, boolean success, String errorMessage) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDetails(details);
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);

            enrichWithContext(log);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Log audit entry with username override (for login events)
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void logWithUsername(AuditAction action, String username, 
                                boolean success, String errorMessage) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setEntityType(AuditEntityType.USER);
            log.setUsername(username == null ? null : username.substring(0, Math.min(username.length(), 50)));
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);

            enrichWithRequest(log);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Enrich log with user and request context
     */
    private void enrichWithContext(AuditLog log) {
        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            log.setUsername(authentication.getName());
            if (authentication.getPrincipal() instanceof com.campushub.common.security.UserPrincipal principal) {
                log.setUserId(principal.getId());
            }
        }

        enrichWithRequest(log);
    }

    /**
     * Enrich log with HTTP request information
     */
    private void enrichWithRequest(AuditLog log) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.setIpAddress(getClientIp(request));
                String agent = request.getHeader("User-Agent");
                log.setUserAgent(agent == null ? null : agent.substring(0, Math.min(agent.length(), 500)));
            }
        } catch (Exception e) {
            logger.debug("Could not enrich audit log with request context: {}", e.getMessage());
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip;
    }

    /**
     * Query audit logs with filters
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findWithFilters(
            Long userId, AuditAction action, AuditEntityType entityType,
            Boolean success, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        return auditLogRepository.findWithFilters(userId, action, entityType, success, startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get audit logs for specific entity
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByEntity(AuditEntityType entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get audit logs for specific user
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get statistics
     */
    @Transactional(readOnly = true)
    public long countFailedActionSince(AuditAction action, LocalDateTime since) {
        return auditLogRepository.countFailedByActionSince(action, since);
    }

    @Transactional(readOnly = true)
    public long countFailedSince(LocalDateTime since) {
        return auditLogRepository.countFailedSince(since);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setUsername(log.getUsername());
        response.setAction(log.getAction());
        response.setEntityType(log.getEntityType());
        response.setEntityId(log.getEntityId());
        response.setDetails(log.getDetails());
        response.setIpAddress(log.getIpAddress());
        response.setSuccess(log.getSuccess());
        response.setErrorMessage(log.getErrorMessage());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}

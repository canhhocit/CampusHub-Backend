package com.campushub.modules.audit.controller;

import com.campushub.modules.audit.dto.AuditLogResponse;
import com.campushub.modules.audit.entity.AuditAction;
import com.campushub.modules.audit.entity.AuditEntityType;
import com.campushub.modules.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Logs", description = "Audit log management APIs (Admin only)")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Get audit logs", description = "Get audit logs with filters (admin only)")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 100) size = 100;

        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponse> logs = auditService.findWithFilters(
                userId, action, entityType, success, startDate, endDate, pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get entity audit trail", description = "Get all audit logs for a specific entity")
    public ResponseEntity<Page<AuditLogResponse>> getEntityAuditTrail(
            @PathVariable AuditEntityType entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponse> logs = auditService.findByEntity(entityType, entityId, pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user audit trail", description = "Get all audit logs for a specific user")
    public ResponseEntity<Page<AuditLogResponse>> getUserAuditTrail(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponse> logs = auditService.findByUser(userId, pageable);

        return ResponseEntity.ok(logs);
    }
}

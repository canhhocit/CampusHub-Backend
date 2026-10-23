package com.campushub.modules.identity.service;

import com.campushub.modules.audit.entity.AuditAction;
import com.campushub.modules.audit.entity.AuditEntityType;
import com.campushub.modules.audit.service.AuditService;
import com.campushub.modules.identity.config.IdentityProperties;
import com.campushub.modules.identity.entity.IdentityVerificationAttempt;
import com.campushub.modules.identity.enums.VerificationFailureCode;
import com.campushub.modules.identity.repository.IdentityVerificationAttemptRepository;
import com.campushub.modules.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dedicated component for persisting failed eKYC attempts and audit trails
 * in an autonomous transaction (REQUIRES_NEW).
 *
 * Ensures audit logging and failed attempts are reliably persisted
 * even when outer business methods roll back upon throwing AppException.
 */
@Component
public class IdentityFailureRecorder {

    private static final Logger log = LoggerFactory.getLogger(IdentityFailureRecorder.class);

    private final IdentityVerificationAttemptRepository attemptRepository;
    private final AuditService auditService;
    private final IdentityProperties properties;

    public IdentityFailureRecorder(
            IdentityVerificationAttemptRepository attemptRepository,
            AuditService auditService,
            IdentityProperties properties) {
        this.attemptRepository = attemptRepository;
        this.auditService = auditService;
        this.properties = properties;
    }

    /**
     * Records a new failed attempt during the startAttempt phase in an autonomous transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdentityVerificationAttempt recordInitialFailure(
            User user, String cardHash, String citizenId,
            VerificationFailureCode code, String reason) {
        IdentityVerificationAttempt attempt = new IdentityVerificationAttempt();
        attempt.setAttemptId(UUID.randomUUID().toString());
        attempt.setUser(user);
        attempt.setCardPhotoHash(cardHash);
        attempt.setCitizenId(citizenId);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getAttemptTtlMinutes()));
        attempt.markFailed(code, reason);

        attemptRepository.save(attempt);
        attemptRepository.flush();
        auditService.log(AuditAction.IDENTITY_ATTEMPT_FAILED, AuditEntityType.USER, user.getId(),
                "Initial eKYC attempt failed: " + code + " - " + reason);
        log.info("Persisted failed initial eKYC attempt {} for user {}", attempt.getAttemptId(), user.getUsername());
        return attempt;
    }

    /**
     * Updates an existing in-progress attempt to failed in an autonomous transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStepFailure(
            String attemptId, VerificationFailureCode code, String reason, String antiSpoofingOutcome) {
        attemptRepository.findByAttemptId(attemptId).ifPresent(attempt -> {
            attempt.markFailed(code, reason);
            if (antiSpoofingOutcome != null) {
                attempt.setAntiSpoofingOutcome(antiSpoofingOutcome);
            }
            attemptRepository.save(attempt);
            attemptRepository.flush();
            auditService.log(AuditAction.IDENTITY_ATTEMPT_FAILED, AuditEntityType.USER, attempt.getUser().getId(),
                    "eKYC attempt " + attemptId + " failed: " + code + " - " + reason);
            log.info("Persisted failed step for attempt {}: code={}, reason={}", attemptId, code, reason);
        });
    }

    /**
     * Marks an existing attempt as EXPIRED in an autonomous transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordExpired(String attemptId) {
        attemptRepository.findByAttemptId(attemptId).ifPresent(attempt -> {
            attempt.markExpired();
            attemptRepository.save(attempt);
            attemptRepository.flush();
            auditService.log(AuditAction.IDENTITY_ATTEMPT_FAILED, AuditEntityType.USER, attempt.getUser().getId(),
                    "eKYC attempt " + attemptId + " marked as expired");
            log.info("Persisted expired status for attempt {}", attemptId);
        });
    }
}

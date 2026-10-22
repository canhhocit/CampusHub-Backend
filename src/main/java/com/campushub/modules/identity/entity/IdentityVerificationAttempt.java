package com.campushub.modules.identity.entity;

import com.campushub.common.model.BaseEntity;
import com.campushub.modules.identity.enums.VerificationAttemptStatus;
import com.campushub.modules.identity.enums.VerificationFailureCode;
import com.campushub.modules.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "identity_verification_attempts")
public class IdentityVerificationAttempt extends BaseEntity {

    @Column(name = "attempt_id", unique = true, nullable = false, length = 64)
    private String attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VerificationAttemptStatus status;

    @Column(name = "liveness_session_id", length = 100)
    private String livenessSessionId;

    @Column(name = "citizen_id", length = 20)
    private String citizenId;

    @Column(name = "card_photo_hash", length = 64)
    private String cardPhotoHash;

    @Column(name = "face_similarity_score")
    private Double faceSimilarityScore;

    @Column(name = "face_threshold")
    private Double faceThreshold;

    @Column(name = "face_matched")
    private Boolean faceMatched;

    @Column(name = "liveness_passed")
    private Boolean livenessPassed;

    @Column(name = "anti_spoofing_outcome", length = 30)
    private String antiSpoofingOutcome;

    @Column(name = "step_count")
    private Integer stepCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 50)
    private VerificationFailureCode failureCode;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "id_card_front_url", length = 500)
    private String idCardFrontUrl;

    @Column(name = "selfie_url", length = 500)
    private String selfieUrl;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public IdentityVerificationAttempt() {
    }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void markFailed(VerificationFailureCode code, String reason) {
        this.status = VerificationAttemptStatus.REJECTED;
        this.failureCode = code;
        this.failureReason = reason != null && reason.length() > 250 ? reason.substring(0, 250) : reason;
        this.completedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = VerificationAttemptStatus.EXPIRED;
        this.failureCode = VerificationFailureCode.ATTEMPT_EXPIRED;
        this.failureReason = "Phiên xác minh đã hết thời gian thực hiện";
        this.completedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VerificationAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationAttemptStatus status) {
        this.status = status;
    }

    public String getLivenessSessionId() {
        return livenessSessionId;
    }

    public void setLivenessSessionId(String livenessSessionId) {
        this.livenessSessionId = livenessSessionId;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getCardPhotoHash() {
        return cardPhotoHash;
    }

    public void setCardPhotoHash(String cardPhotoHash) {
        this.cardPhotoHash = cardPhotoHash;
    }

    public Double getFaceSimilarityScore() {
        return faceSimilarityScore;
    }

    public void setFaceSimilarityScore(Double faceSimilarityScore) {
        this.faceSimilarityScore = faceSimilarityScore;
    }

    public Double getFaceThreshold() {
        return faceThreshold;
    }

    public void setFaceThreshold(Double faceThreshold) {
        this.faceThreshold = faceThreshold;
    }

    public Boolean getFaceMatched() {
        return faceMatched;
    }

    public void setFaceMatched(Boolean faceMatched) {
        this.faceMatched = faceMatched;
    }

    public Boolean getLivenessPassed() {
        return livenessPassed;
    }

    public void setLivenessPassed(Boolean livenessPassed) {
        this.livenessPassed = livenessPassed;
    }

    public String getAntiSpoofingOutcome() {
        return antiSpoofingOutcome;
    }

    public void setAntiSpoofingOutcome(String antiSpoofingOutcome) {
        this.antiSpoofingOutcome = antiSpoofingOutcome;
    }

    public Integer getStepCount() {
        return stepCount != null ? stepCount : 0;
    }

    public void setStepCount(Integer stepCount) {
        this.stepCount = stepCount;
    }

    public VerificationFailureCode getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(VerificationFailureCode failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getIdCardFrontUrl() {
        return idCardFrontUrl;
    }

    public void setIdCardFrontUrl(String idCardFrontUrl) {
        this.idCardFrontUrl = idCardFrontUrl;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }

    public void setSelfieUrl(String selfieUrl) {
        this.selfieUrl = selfieUrl;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}

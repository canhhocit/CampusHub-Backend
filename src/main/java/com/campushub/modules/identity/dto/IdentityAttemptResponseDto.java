package com.campushub.modules.identity.dto;

import com.campushub.modules.identity.enums.VerificationAttemptStatus;
import com.campushub.modules.identity.enums.VerificationFailureCode;

import java.time.LocalDateTime;

public class IdentityAttemptResponseDto {

    private String attemptId;
    private VerificationAttemptStatus status;
    private String citizenIdMasked;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    private Double faceSimilarityScore;
    private Double faceThreshold;
    private Boolean faceMatched;
    private Boolean livenessPassed;
    private String antiSpoofingOutcome;
    private VerificationFailureCode failureCode;
    private String failureReason;

    public IdentityAttemptResponseDto() {
    }

    public static String maskCitizenId(String citizenId) {
        if (citizenId == null || citizenId.length() < 7) {
            return citizenId;
        }
        int len = citizenId.length();
        return citizenId.substring(0, 3) + "******" + citizenId.substring(len - 3);
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public VerificationAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationAttemptStatus status) {
        this.status = status;
    }

    public String getCitizenIdMasked() {
        return citizenIdMasked;
    }

    public void setCitizenIdMasked(String citizenIdMasked) {
        this.citizenIdMasked = citizenIdMasked;
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
}

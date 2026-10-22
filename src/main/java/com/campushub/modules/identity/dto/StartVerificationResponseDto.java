package com.campushub.modules.identity.dto;

import com.campushub.infrastructure.ocr.dto.ChallengeInfoDto;
import com.campushub.modules.identity.enums.VerificationAttemptStatus;

import java.time.LocalDateTime;

public class StartVerificationResponseDto {

    private String attemptId;
    private VerificationAttemptStatus status;
    private LocalDateTime expiresAt;
    private ChallengeInfoDto currentChallenge;
    private int totalChallenges;
    private int stepIndex;

    public StartVerificationResponseDto() {
    }

    public StartVerificationResponseDto(String attemptId, VerificationAttemptStatus status, LocalDateTime expiresAt,
                                        ChallengeInfoDto currentChallenge, int totalChallenges, int stepIndex) {
        this.attemptId = attemptId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.currentChallenge = currentChallenge;
        this.totalChallenges = totalChallenges;
        this.stepIndex = stepIndex;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public ChallengeInfoDto getCurrentChallenge() {
        return currentChallenge;
    }

    public void setCurrentChallenge(ChallengeInfoDto currentChallenge) {
        this.currentChallenge = currentChallenge;
    }

    public int getTotalChallenges() {
        return totalChallenges;
    }

    public void setTotalChallenges(int totalChallenges) {
        this.totalChallenges = totalChallenges;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }
}

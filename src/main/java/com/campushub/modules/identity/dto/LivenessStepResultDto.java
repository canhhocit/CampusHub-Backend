package com.campushub.modules.identity.dto;

import com.campushub.infrastructure.ocr.dto.ChallengeInfoDto;
import com.campushub.modules.identity.enums.VerificationAttemptStatus;

public class LivenessStepResultDto {

    private String attemptId;
    private VerificationAttemptStatus status;
    private int stepIndex;
    private boolean actionCompleted;
    private boolean livenessPassed;
    private ChallengeInfoDto nextChallenge;
    private String message;

    public LivenessStepResultDto() {
    }

    public LivenessStepResultDto(String attemptId, VerificationAttemptStatus status, int stepIndex,
                                 boolean actionCompleted, boolean livenessPassed,
                                 ChallengeInfoDto nextChallenge, String message) {
        this.attemptId = attemptId;
        this.status = status;
        this.stepIndex = stepIndex;
        this.actionCompleted = actionCompleted;
        this.livenessPassed = livenessPassed;
        this.nextChallenge = nextChallenge;
        this.message = message;
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

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public boolean isActionCompleted() {
        return actionCompleted;
    }

    public void setActionCompleted(boolean actionCompleted) {
        this.actionCompleted = actionCompleted;
    }

    public boolean isLivenessPassed() {
        return livenessPassed;
    }

    public void setLivenessPassed(boolean livenessPassed) {
        this.livenessPassed = livenessPassed;
    }

    public ChallengeInfoDto getNextChallenge() {
        return nextChallenge;
    }

    public void setNextChallenge(ChallengeInfoDto nextChallenge) {
        this.nextChallenge = nextChallenge;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

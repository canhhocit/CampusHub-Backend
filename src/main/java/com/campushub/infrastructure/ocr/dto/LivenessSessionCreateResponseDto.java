package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps LivenessSessionCreateResponse in app/schemas/liveness.py from POST /api/v1/liveness/session/create.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivenessSessionCreateResponseDto {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("created_at")
    private Double createdAt;

    @JsonProperty("expires_in_seconds")
    private Integer expiresInSeconds;

    @JsonProperty("total_steps")
    private Integer totalSteps;

    @JsonProperty("current_step")
    private Integer currentStep;

    private ChallengeInfoDto challenge;

    public LivenessSessionCreateResponseDto() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Double getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Double createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Integer expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public ChallengeInfoDto getChallenge() {
        return challenge;
    }

    public void setChallenge(ChallengeInfoDto challenge) {
        this.challenge = challenge;
    }
}

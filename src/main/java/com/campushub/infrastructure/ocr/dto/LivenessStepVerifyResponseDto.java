package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Maps LivenessStepVerifyResponse in app/schemas/liveness.py from POST /api/v1/liveness/session/{session_id}/verify-step.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivenessStepVerifyResponseDto {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("step_passed")
    private Boolean stepPassed;

    @JsonProperty("current_step")
    private Integer currentStep;

    @JsonProperty("total_steps")
    private Integer totalSteps;

    @JsonProperty("all_passed")
    private Boolean allPassed;

    private String status;

    @JsonProperty("next_challenge")
    private ChallengeInfoDto nextChallenge;

    @JsonProperty("reason_code")
    private String reasonCode;

    private String message;

    private Map<String, Object> metrics;

    @JsonProperty("anti_spoofing_outcome")
    private String antiSpoofingOutcome;

    public LivenessStepVerifyResponseDto() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getStepPassed() {
        return stepPassed;
    }

    public void setStepPassed(Boolean stepPassed) {
        this.stepPassed = stepPassed;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Boolean getAllPassed() {
        return allPassed;
    }

    public void setAllPassed(Boolean allPassed) {
        this.allPassed = allPassed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ChallengeInfoDto getNextChallenge() {
        return nextChallenge;
    }

    public void setNextChallenge(ChallengeInfoDto nextChallenge) {
        this.nextChallenge = nextChallenge;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public String getAntiSpoofingOutcome() {
        return antiSpoofingOutcome;
    }

    public void setAntiSpoofingOutcome(String antiSpoofingOutcome) {
        this.antiSpoofingOutcome = antiSpoofingOutcome;
    }
}

package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Maps LivenessStepVerifyResponse in app/schemas/liveness.py from POST /api/v1/liveness/session/{session_id}/verify-step.
 *
 * NOTE ON LIVENESS & PAD EVALUATION:
 * - A single step passing does NOT mean the entire session is verified (check isAllPassed() and status == PASSED).
 * - Active challenge compliance (yaw movement) can NEVER bypass passive PAD: if anti_spoofing_outcome != LIVE,
 *   the step is not considered genuinely live.
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

    private LivenessSessionStatus status;

    @JsonProperty("next_challenge")
    private ChallengeInfoDto nextChallenge;

    @JsonProperty("reason_code")
    private String reasonCode;

    private String message;

    private Map<String, Object> metrics;

    @JsonProperty("anti_spoofing_outcome")
    private AntiSpoofingOutcome antiSpoofingOutcome;

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

    public LivenessSessionStatus getStatus() {
        return status != null ? status : LivenessSessionStatus.UNKNOWN;
    }

    public void setStatus(LivenessSessionStatus status) {
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

    public AntiSpoofingOutcome getAntiSpoofingOutcome() {
        return antiSpoofingOutcome != null ? antiSpoofingOutcome : AntiSpoofingOutcome.UNKNOWN;
    }

    public void setAntiSpoofingOutcome(AntiSpoofingOutcome antiSpoofingOutcome) {
        this.antiSpoofingOutcome = antiSpoofingOutcome;
    }

    /**
     * Fail-closed check: Step is only successful if step_passed is true AND anti_spoofing_outcome is LIVE.
     */
    public boolean isStepSuccessful() {
        return Boolean.TRUE.equals(stepPassed)
                && antiSpoofingOutcome == AntiSpoofingOutcome.LIVE;
    }

    /**
     * Strict check for complete session success: all_passed is true, session status is PASSED,
     * and anti_spoofing_outcome is LIVE.
     */
    public boolean isSessionFullyPassed() {
        return Boolean.TRUE.equals(allPassed)
                && status == LivenessSessionStatus.PASSED
                && antiSpoofingOutcome == AntiSpoofingOutcome.LIVE;
    }

    public boolean isSpoofDetected() {
        return antiSpoofingOutcome == AntiSpoofingOutcome.SPOOF;
    }
}

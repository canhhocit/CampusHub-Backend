package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps LivenessSessionStatusResponse in Python app/schemas/liveness.py from GET /api/v1/liveness/session/{session_id}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivenessSessionStatusResponseDto {

    @JsonProperty("session_id")
    private String sessionId;

    private LivenessSessionStatus status;

    @JsonProperty("current_step")
    private Integer currentStep;

    @JsonProperty("total_steps")
    private Integer totalSteps;

    private Boolean passed;

    @JsonProperty("created_at")
    private Double createdAt;

    @JsonProperty("expires_at")
    private Double expiresAt;

    private List<Map<String, Object>> history;

    private String disclaimer;

    public LivenessSessionStatusResponseDto() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LivenessSessionStatus getStatus() {
        return status;
    }

    public void setStatus(LivenessSessionStatus status) {
        this.status = status;
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

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Double getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Double createdAt) {
        this.createdAt = createdAt;
    }

    public Double getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Double expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<Map<String, Object>> getHistory() {
        return history != null ? history : Collections.emptyList();
    }

    public void setHistory(List<Map<String, Object>> history) {
        this.history = history;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    /**
     * Strict check for verified session completion: status must be PASSED and passed boolean must be true.
     */
    public boolean isSessionPassed() {
        return Boolean.TRUE.equals(passed) && status == LivenessSessionStatus.PASSED;
    }
}

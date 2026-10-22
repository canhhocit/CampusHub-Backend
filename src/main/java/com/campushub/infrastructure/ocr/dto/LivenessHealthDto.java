package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Maps the Python LivenessHealthResponse schema from GET /api/v1/liveness/health.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivenessHealthDto {

    private String status;
    private String version;

    @JsonProperty("active_liveness_ready")
    private Boolean activeLivenessReady;

    @JsonProperty("passive_anti_spoofing_ready")
    private Boolean passiveAntiSpoofingReady;

    @JsonProperty("anti_spoofing_enabled")
    private Boolean antiSpoofingEnabled;

    @JsonProperty("fail_closed")
    private Boolean failClosed;

    private Map<String, Double> thresholds;
    private Map<String, Object> models;

    @JsonProperty("error_message")
    private String errorMessage;

    public LivenessHealthDto() {
    }

    public LivenessHealthDto(String status, String version, Boolean activeLivenessReady,
                             Boolean passiveAntiSpoofingReady, Boolean antiSpoofingEnabled,
                             Boolean failClosed, Map<String, Double> thresholds,
                             Map<String, Object> models, String errorMessage) {
        this.status = status;
        this.version = version;
        this.activeLivenessReady = activeLivenessReady;
        this.passiveAntiSpoofingReady = passiveAntiSpoofingReady;
        this.antiSpoofingEnabled = antiSpoofingEnabled;
        this.failClosed = failClosed;
        this.thresholds = thresholds;
        this.models = models;
        this.errorMessage = errorMessage;
    }

    public boolean isActiveLivenessReady() {
        return Boolean.TRUE.equals(activeLivenessReady);
    }

    public boolean isPassiveAntiSpoofingReady() {
        return Boolean.TRUE.equals(passiveAntiSpoofingReady);
    }

    public boolean isAntiSpoofingEnabled() {
        return Boolean.TRUE.equals(antiSpoofingEnabled);
    }

    public boolean isFailClosed() {
        return Boolean.TRUE.equals(failClosed);
    }

    public boolean isHealthy() {
        return "ok".equalsIgnoreCase(status) && isActiveLivenessReady() && isPassiveAntiSpoofingReady();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getActiveLivenessReady() {
        return activeLivenessReady;
    }

    public void setActiveLivenessReady(Boolean activeLivenessReady) {
        this.activeLivenessReady = activeLivenessReady;
    }

    public Boolean getPassiveAntiSpoofingReady() {
        return passiveAntiSpoofingReady;
    }

    public void setPassiveAntiSpoofingReady(Boolean passiveAntiSpoofingReady) {
        this.passiveAntiSpoofingReady = passiveAntiSpoofingReady;
    }

    public Boolean getAntiSpoofingEnabled() {
        return antiSpoofingEnabled;
    }

    public void setAntiSpoofingEnabled(Boolean antiSpoofingEnabled) {
        this.antiSpoofingEnabled = antiSpoofingEnabled;
    }

    public Boolean getFailClosed() {
        return failClosed;
    }

    public void setFailClosed(Boolean failClosed) {
        this.failClosed = failClosed;
    }

    public Map<String, Double> getThresholds() {
        return thresholds != null ? thresholds : Collections.emptyMap();
    }

    public void setThresholds(Map<String, Double> thresholds) {
        this.thresholds = thresholds;
    }

    public Map<String, Object> getModels() {
        return models != null ? models : Collections.emptyMap();
    }

    public void setModels(Map<String, Object> models) {
        this.models = models;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "LivenessHealthDto{" +
                "status='" + status + '\'' +
                ", version='" + version + '\'' +
                ", activeLivenessReady=" + activeLivenessReady +
                ", passiveAntiSpoofingReady=" + passiveAntiSpoofingReady +
                ", antiSpoofingEnabled=" + antiSpoofingEnabled +
                ", failClosed=" + failClosed +
                ", thresholds=" + thresholds +
                ", models=" + models +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

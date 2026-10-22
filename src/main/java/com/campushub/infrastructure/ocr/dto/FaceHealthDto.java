package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the Python FaceHealthResponse schema from GET /api/v1/face/health.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaceHealthDto {

    private String status;
    private String version;
    private String detector;
    private String recognizer;
    private String device;

    @JsonProperty("models_loaded")
    private Boolean modelsLoaded;

    private Double threshold;

    @JsonProperty("engine_type")
    private String engineType;

    @JsonProperty("error_message")
    private String errorMessage;

    public FaceHealthDto() {
    }

    public FaceHealthDto(String status, String version, String detector, String recognizer,
                         String device, Boolean modelsLoaded, Double threshold,
                         String engineType, String errorMessage) {
        this.status = status;
        this.version = version;
        this.detector = detector;
        this.recognizer = recognizer;
        this.device = device;
        this.modelsLoaded = modelsLoaded;
        this.threshold = threshold;
        this.engineType = engineType;
        this.errorMessage = errorMessage;
    }

    public boolean isModelsLoaded() {
        return Boolean.TRUE.equals(modelsLoaded);
    }

    public boolean isHealthy() {
        return "ok".equalsIgnoreCase(status) && isModelsLoaded();
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

    public String getDetector() {
        return detector;
    }

    public void setDetector(String detector) {
        this.detector = detector;
    }

    public String getRecognizer() {
        return recognizer;
    }

    public void setRecognizer(String recognizer) {
        this.recognizer = recognizer;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Boolean getModelsLoaded() {
        return modelsLoaded;
    }

    public void setModelsLoaded(Boolean modelsLoaded) {
        this.modelsLoaded = modelsLoaded;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "FaceHealthDto{" +
                "status='" + status + '\'' +
                ", version='" + version + '\'' +
                ", detector='" + detector + '\'' +
                ", recognizer='" + recognizer + '\'' +
                ", device='" + device + '\'' +
                ", modelsLoaded=" + modelsLoaded +
                ", threshold=" + threshold +
                ", engineType='" + engineType + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

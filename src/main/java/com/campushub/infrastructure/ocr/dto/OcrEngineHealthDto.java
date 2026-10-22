package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Maps the Python HealthResponse schema from GET /api/v1/health.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrEngineHealthDto {

    private String status;
    private String version;

    @JsonProperty("ocr_engine")
    private String ocrEngine;

    private String device;

    @JsonProperty("supported_formats")
    private List<String> supportedFormats;

    public OcrEngineHealthDto() {
    }

    public OcrEngineHealthDto(String status, String version, String ocrEngine, String device, List<String> supportedFormats) {
        this.status = status;
        this.version = version;
        this.ocrEngine = ocrEngine;
        this.device = device;
        this.supportedFormats = supportedFormats;
    }

    public boolean isHealthy() {
        return "ok".equalsIgnoreCase(status);
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

    public String getOcrEngine() {
        return ocrEngine;
    }

    public void setOcrEngine(String ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public List<String> getSupportedFormats() {
        return supportedFormats != null ? supportedFormats : Collections.emptyList();
    }

    public void setSupportedFormats(List<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    @Override
    public String toString() {
        return "OcrEngineHealthDto{" +
                "status='" + status + '\'' +
                ", version='" + version + '\'' +
                ", ocrEngine='" + ocrEngine + '\'' +
                ", device='" + device + '\'' +
                ", supportedFormats=" + supportedFormats +
                '}';
    }
}

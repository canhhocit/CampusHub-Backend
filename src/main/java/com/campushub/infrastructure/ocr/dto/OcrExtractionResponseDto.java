package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps OCRResponse in app/schemas/ocr.py from POST /api/v1/ocr.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrExtractionResponseDto {

    private boolean success;
    private String side;

    @JsonProperty("processing_time_ms")
    private Double processingTimeMs;

    private Map<String, ExtractedFieldDto> fields;
    private List<String> warnings;
    private String disclaimer;

    public OcrExtractionResponseDto() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Double getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Double processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Map<String, ExtractedFieldDto> getFields() {
        return fields != null ? fields : Collections.emptyMap();
    }

    public void setFields(Map<String, ExtractedFieldDto> fields) {
        this.fields = fields;
    }

    public List<String> getWarnings() {
        return warnings != null ? warnings : Collections.emptyList();
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}

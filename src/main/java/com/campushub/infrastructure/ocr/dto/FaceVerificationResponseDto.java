package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Maps FaceVerificationResponse in app/schemas/face.py from POST /api/v1/face/verify.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaceVerificationResponseDto {

    private boolean success;
    private boolean match;

    @JsonProperty("similarity_score")
    private Double similarityScore;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    private Double threshold;

    @JsonProperty("pipeline_state")
    private String pipelineState;

    private List<String> warnings;

    @JsonProperty("processing_time_ms")
    private Double processingTimeMs;

    private String disclaimer;

    public FaceVerificationResponseDto() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getPipelineState() {
        return pipelineState;
    }

    public void setPipelineState(String pipelineState) {
        this.pipelineState = pipelineState;
    }

    public List<String> getWarnings() {
        return warnings != null ? warnings : Collections.emptyList();
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public Double getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Double processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}

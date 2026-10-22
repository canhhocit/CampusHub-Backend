package com.campushub.modules.identity.dto;

import com.campushub.modules.identity.enums.VerificationAttemptStatus;

public class FaceVerificationResultDto {

    private String attemptId;
    private VerificationAttemptStatus status;
    private Double similarityScore;
    private Double threshold;
    private Boolean match;
    private String message;

    public FaceVerificationResultDto() {
    }

    public FaceVerificationResultDto(String attemptId, VerificationAttemptStatus status,
                                    Double similarityScore, Double threshold,
                                    Boolean match, String message) {
        this.attemptId = attemptId;
        this.status = status;
        this.similarityScore = similarityScore;
        this.threshold = threshold;
        this.match = match;
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

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Boolean getMatch() {
        return match;
    }

    public void setMatch(Boolean match) {
        this.match = match;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

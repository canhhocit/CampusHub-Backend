package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status of a liveness session. Maps session status in Python app/schemas/liveness.py.
 */
public enum LivenessSessionStatus {
    IN_PROGRESS,
    PASSED,
    FAILED,
    EXPIRED,
    UNKNOWN;

    @JsonCreator
    public static LivenessSessionStatus fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return LivenessSessionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public boolean isPassed() {
        return this == PASSED;
    }

    public boolean isTerminal() {
        return this == PASSED || this == FAILED || this == EXPIRED;
    }
}

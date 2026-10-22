package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Extraction status of an individual CCCD field. Maps FieldStatus in Python app/schemas/ocr.py.
 */
public enum FieldStatus {
    EXTRACTED,
    NEEDS_REVIEW,
    MISSING,
    INVALID,
    UNKNOWN;

    @JsonCreator
    public static FieldStatus fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        String upper = value.trim().toUpperCase();
        return switch (upper) {
            case "EXTRACTED", "VALID" -> EXTRACTED;
            case "NEEDS_REVIEW" -> NEEDS_REVIEW;
            case "MISSING", "NOT_DETECTED" -> MISSING;
            case "INVALID", "UNREADABLE" -> INVALID;
            default -> UNKNOWN;
        };
    }

    public boolean isExtracted() {
        return this == EXTRACTED;
    }
}

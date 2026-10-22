package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Outcome of Passive Face Anti-Spoofing (PAD) classification. Maps AntiSpoofingOutcome in Python app/schemas/liveness.py.
 */
public enum AntiSpoofingOutcome {
    LIVE,
    SPOOF,
    UNCERTAIN,
    ERROR,
    UNKNOWN;

    @JsonCreator
    public static AntiSpoofingOutcome fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return AntiSpoofingOutcome.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * Strict fail-closed check: ONLY returns true if definitely LIVE.
     */
    public boolean isLive() {
        return this == LIVE;
    }

    public boolean isSpoof() {
        return this == SPOOF;
    }
}

package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Expected action for active liveness challenge step. Maps ChallengeAction in Python app/schemas/liveness.py.
 */
public enum ChallengeAction {
    TURN_HEAD_LEFT,
    TURN_HEAD_RIGHT,
    NOD_HEAD,
    FACE_CENTER,
    UNKNOWN;

    @JsonCreator
    public static ChallengeAction fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return ChallengeAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}

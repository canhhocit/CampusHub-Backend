package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Side of the CCCD card. Maps CardSide enum in Python app/schemas/ocr.py.
 */
public enum CardSide {
    FRONT,
    BACK,
    AUTO,
    UNKNOWN;

    @JsonCreator
    public static CardSide fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return CardSide.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}

package com.campushub.infrastructure.ocr.exception;

/**
 * Thrown when the OCR service returns a response that cannot be parsed (e.g. malformed JSON, missing required fields).
 */
public class OcrInvalidResponseException extends OcrIntegrationException {

    public OcrInvalidResponseException(String message) {
        super(message);
    }

    public OcrInvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}

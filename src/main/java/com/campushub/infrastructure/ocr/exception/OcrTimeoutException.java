package com.campushub.infrastructure.ocr.exception;

/**
 * Thrown when an HTTP request to the OCR service exceeds the configured connection or read timeout.
 */
public class OcrTimeoutException extends OcrIntegrationException {

    public OcrTimeoutException(String message) {
        super(message);
    }

    public OcrTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

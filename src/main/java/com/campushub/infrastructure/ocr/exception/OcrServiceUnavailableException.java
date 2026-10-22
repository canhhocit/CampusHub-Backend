package com.campushub.infrastructure.ocr.exception;

/**
 * Thrown when the OCR service or OCR engine itself is reported as unavailable or disabled.
 */
public class OcrServiceUnavailableException extends OcrIntegrationException {

    public OcrServiceUnavailableException(String message) {
        super(message);
    }

    public OcrServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

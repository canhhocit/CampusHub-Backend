package com.campushub.infrastructure.ocr.exception;

/**
 * Base exception for all OCR and Biometrics integration failures.
 * Designed to ensure sensitive biometric data, image buffers, and credentials
 * are never included in exception messages or stack traces.
 */
public class OcrIntegrationException extends RuntimeException {

    public OcrIntegrationException(String message) {
        super(message);
    }

    public OcrIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

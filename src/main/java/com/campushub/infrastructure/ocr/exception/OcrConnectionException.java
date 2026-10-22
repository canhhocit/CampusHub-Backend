package com.campushub.infrastructure.ocr.exception;

/**
 * Thrown when the backend fails to connect to the OCR service (e.g., connection refused, host unreachable, DNS resolution error).
 */
public class OcrConnectionException extends OcrIntegrationException {

    public OcrConnectionException(String message) {
        super(message);
    }

    public OcrConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

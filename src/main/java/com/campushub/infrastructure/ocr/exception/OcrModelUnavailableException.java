package com.campushub.infrastructure.ocr.exception;

/**
 * Thrown when the OCR service is reachable, but one or more required AI models
 * (YuNet, SFace, MiniFASNet V2, MiniFASNet V1SE) are reported as not loaded or unavailable.
 */
public class OcrModelUnavailableException extends OcrIntegrationException {

    private final Object healthReport;

    public OcrModelUnavailableException(String message) {
        super(message);
        this.healthReport = null;
    }

    public OcrModelUnavailableException(String message, Object healthReport) {
        super(message);
        this.healthReport = healthReport;
    }

    public Object getHealthReport() {
        return healthReport;
    }
}

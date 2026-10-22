package com.campushub.infrastructure.ocr.exception;

/**
 * Thrown when the OCR service responds with a non-2xx HTTP status code (e.g. 400, 404, 500, 503).
 */
public class OcrServerException extends OcrIntegrationException {

    private final int statusCode;
    private final String responseBody;

    public OcrServerException(int statusCode, String responseBody, String message) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = sanitizeResponseBody(responseBody);
    }

    public OcrServerException(int statusCode, String responseBody, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = sanitizeResponseBody(responseBody);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private static String sanitizeResponseBody(String raw) {
        if (raw == null) {
            return "";
        }
        // Limit body length to prevent memory bloating or accidental logging of large payloads
        if (raw.length() > 500) {
            return raw.substring(0, 500) + "... [truncated]";
        }
        return raw;
    }
}

package com.campushub.infrastructure.ocr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for external OCR & Biometrics FastAPI Service.
 * Prefix supported: "app.ocr" (and mapped to environment variables like OCR_SERVICE_URL).
 */
@ConfigurationProperties(prefix = "app.ocr")
public class OcrProperties {

    /**
     * Base URL of the Python OCR & Biometrics service.
     * Default: http://localhost:8081
     */
    private String baseUrl = "http://localhost:8081";

    /**
     * Connection timeout in milliseconds.
     * Default: 2000 ms (2 seconds)
     */
    private int connectTimeoutMs = 2000;

    /**
     * Read / response timeout in milliseconds.
     * Default: 8000 ms (8 seconds)
     */
    private int readTimeoutMs = 8000;

    public OcrProperties() {
    }

    public OcrProperties(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /**
     * Server-approved threshold for face matching (Cosine similarity threshold in [-1.0, 1.0]).
     * Default: 0.363 (recommended standard by OpenCV Zoo SFace).
     */
    private Double faceMatchThreshold = 0.363;

    public Double getFaceMatchThreshold() {
        return faceMatchThreshold;
    }

    public void setFaceMatchThreshold(Double faceMatchThreshold) {
        this.faceMatchThreshold = faceMatchThreshold;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}

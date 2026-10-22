package com.campushub.infrastructure.ocr.client;

import com.campushub.infrastructure.ocr.config.OcrProperties;
import com.campushub.infrastructure.ocr.dto.FaceHealthDto;
import com.campushub.infrastructure.ocr.dto.LivenessHealthDto;
import com.campushub.infrastructure.ocr.dto.OcrEngineHealthDto;
import com.campushub.infrastructure.ocr.dto.OcrOverallHealthDto;
import com.campushub.infrastructure.ocr.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.function.Supplier;

/**
 * Dedicated HTTP client for communicating with CampusHub-OCR & Biometrics Service (FastAPI).
 * Enforces strict connection/read timeouts, typed DTO mapping, fail-closed handling,
 * and safe logging (zero PII, zero biometric payload logging).
 */
@Service
public class OcrServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceClient.class);

    private final RestClient restClient;
    private final OcrProperties properties;

    public OcrServiceClient(RestClient ocrRestClient, OcrProperties properties) {
        this.restClient = ocrRestClient;
        this.properties = properties;
    }

    /**
     * Checks the health of the PaddleOCR text extraction engine.
     * Endpoint: GET /api/v1/health
     *
     * @return OcrEngineHealthDto containing status, engine name, device, and supported formats.
     * @throws OcrConnectionException        if connection cannot be established
     * @throws OcrTimeoutException           if the request times out
     * @throws OcrServerException            if OCR service responds with non-2xx status
     * @throws OcrInvalidResponseException   if the response body is malformed or unparseable
     */
    public OcrEngineHealthDto getOcrEngineHealth() {
        return execute("GET /api/v1/health", () ->
                restClient.get()
                        .uri("/api/v1/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(OcrEngineHealthDto.class)
        );
    }

    /**
     * Asserts that the OCR engine is healthy, otherwise throws OcrServiceUnavailableException.
     */
    public OcrEngineHealthDto assertOcrEngineHealthy() {
        OcrEngineHealthDto health = getOcrEngineHealth();
        if (health == null || !health.isHealthy()) {
            String status = health != null ? health.getStatus() : "null";
            log.warn("OCR engine reported unhealthy status: {}", status);
            throw new OcrServiceUnavailableException("OCR engine reported unhealthy status: " + status);
        }
        return health;
    }

    /**
     * Checks the health and loading status of face recognition models (YuNet + SFace).
     * Endpoint: GET /api/v1/face/health
     *
     * @return FaceHealthDto containing model loading flags, threshold, and engine type.
     * @throws OcrConnectionException        if connection cannot be established
     * @throws OcrTimeoutException           if the request times out
     * @throws OcrServerException            if OCR service responds with non-2xx status
     * @throws OcrInvalidResponseException   if the response body is malformed or unparseable
     */
    public FaceHealthDto getFaceHealth() {
        return execute("GET /api/v1/face/health", () ->
                restClient.get()
                        .uri("/api/v1/face/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(FaceHealthDto.class)
        );
    }

    /**
     * Asserts that face recognition models (YuNet and SFace) are loaded and operational.
     * Throws OcrModelUnavailableException if models are not loaded.
     */
    public FaceHealthDto assertFaceHealthy() {
        FaceHealthDto health = getFaceHealth();
        if (health == null || !health.isHealthy()) {
            String err = (health != null && health.getErrorMessage() != null)
                    ? health.getErrorMessage()
                    : (health != null ? health.getStatus() : "null");
            log.warn("Face verification models reported unavailable: {}", err);
            throw new OcrModelUnavailableException("Face verification models are not ready: " + err, health);
        }
        return health;
    }

    /**
     * Checks the health and readiness of Active Liveness and Passive PAD models (MiniFASNet).
     * Endpoint: GET /api/v1/liveness/health
     *
     * @return LivenessHealthDto with readiness flags, thresholds, and fail_closed status.
     * @throws OcrConnectionException        if connection cannot be established
     * @throws OcrTimeoutException           if the request times out
     * @throws OcrServerException            if OCR service responds with non-2xx status
     * @throws OcrInvalidResponseException   if the response body is malformed or unparseable
     */
    public LivenessHealthDto getLivenessHealth() {
        return execute("GET /api/v1/liveness/health", () ->
                restClient.get()
                        .uri("/api/v1/liveness/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(LivenessHealthDto.class)
        );
    }

    /**
     * Asserts that Active Liveness and Passive Anti-Spoofing models are fully ready.
     * Throws OcrModelUnavailableException if any required liveness/PAD model is missing.
     */
    public LivenessHealthDto assertLivenessHealthy() {
        LivenessHealthDto health = getLivenessHealth();
        if (health == null || !health.isHealthy()) {
            String err = (health != null && health.getErrorMessage() != null)
                    ? health.getErrorMessage()
                    : (health != null ? health.getStatus() : "null");
            log.warn("Liveness models reported unavailable: {}", err);
            throw new OcrModelUnavailableException("Liveness and Anti-Spoofing models are not ready: " + err, health);
        }
        return health;
    }

    /**
     * Comprehensive health check covering all 3 subsystems (OCR, Face, Liveness).
     * Returns an aggregated status object without throwing exceptions on model unavailability.
     */
    public OcrOverallHealthDto checkOverallHealth() {
        OcrEngineHealthDto ocrEngine = null;
        FaceHealthDto face = null;
        LivenessHealthDto liveness = null;
        StringBuilder errorBuilder = new StringBuilder();

        try {
            ocrEngine = getOcrEngineHealth();
            if (ocrEngine == null || !ocrEngine.isHealthy()) {
                errorBuilder.append("OCR Engine not healthy; ");
            }
        } catch (Exception ex) {
            log.warn("Failed checking OCR engine health: {}", ex.getMessage());
            errorBuilder.append("OCR Engine unreachable: ").append(ex.getMessage()).append("; ");
        }

        try {
            face = getFaceHealth();
            if (face == null || !face.isHealthy()) {
                errorBuilder.append("Face models not ready; ");
            }
        } catch (Exception ex) {
            log.warn("Failed checking Face models health: {}", ex.getMessage());
            errorBuilder.append("Face models unreachable: ").append(ex.getMessage()).append("; ");
        }

        try {
            liveness = getLivenessHealth();
            if (liveness == null || !liveness.isHealthy()) {
                errorBuilder.append("Liveness/PAD models not ready; ");
            }
        } catch (Exception ex) {
            log.warn("Failed checking Liveness health: {}", ex.getMessage());
            errorBuilder.append("Liveness unreachable: ").append(ex.getMessage()).append("; ");
        }

        boolean fullyReady = (ocrEngine != null && ocrEngine.isHealthy())
                && (face != null && face.isHealthy())
                && (liveness != null && liveness.isHealthy());

        String message = fullyReady ? "All OCR and Biometrics subsystems operational." : errorBuilder.toString().trim();
        return new OcrOverallHealthDto(fullyReady, ocrEngine, face, liveness, message);
    }

    public OcrProperties getProperties() {
        return properties;
    }

    /**
     * Internal execution wrapper that performs safe logging, timing,
     * and maps low-level network/HTTP errors into clear, typed domain exceptions.
     */
    private <T> T execute(String endpointName, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        log.debug("Initiating OCR service request: [{}]", endpointName);

        try {
            T result = supplier.get();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Completed OCR service request: [{}] in {} ms", endpointName, duration);
            return result;
        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            if (isTimeout(ex)) {
                log.error("OCR service request [{}] timed out after {} ms", endpointName, duration);
                throw new OcrTimeoutException("OCR service request timed out during " + endpointName, ex);
            }
            log.error("Failed to connect to OCR service [{}] after {} ms: {}", endpointName, duration, ex.getMessage());
            throw new OcrConnectionException("Cannot connect to OCR service during " + endpointName, ex);
        } catch (HttpStatusCodeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            HttpStatusCode status = ex.getStatusCode();
            log.error("OCR service [{}] returned HTTP {} after {} ms", endpointName, status.value(), duration);
            throw new OcrServerException(
                    status.value(),
                    ex.getResponseBodyAsString(),
                    "OCR service returned HTTP " + status.value() + " for " + endpointName,
                    ex
            );
        } catch (RestClientException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to parse response from OCR service [{}] after {} ms", endpointName, duration);
            throw new OcrInvalidResponseException("Malformed or unparseable OCR response for " + endpointName, ex);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during OCR service request [{}] after {} ms", endpointName, duration);
            throw new OcrIntegrationException("Unexpected error communicating with OCR service: " + ex.getMessage(), ex);
        }
    }

    private boolean isTimeout(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        String msg = ex.getMessage();
        return msg != null && (msg.toLowerCase().contains("timed out") || msg.toLowerCase().contains("timeout"));
    }
}

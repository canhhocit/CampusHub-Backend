package com.campushub.infrastructure.ocr.client;

import com.campushub.infrastructure.ocr.config.OcrProperties;
import com.campushub.infrastructure.ocr.dto.*;
import com.campushub.infrastructure.ocr.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Dedicated HTTP client for communicating with CampusHub-OCR & Biometrics Service (FastAPI).
 * Enforces strict connection/read timeouts, typed DTO mapping, fail-closed handling,
 * and safe logging (zero PII, zero biometric payload logging).
 */
@Service
public class OcrServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceClient.class);

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;       // 10 MB limit for card/selfie
    private static final int LIVENESS_MIN_FRAMES = 3;                  // Min consecutive frames
    private static final int LIVENESS_MAX_FRAMES = 15;                 // Max consecutive frames
    private static final int LIVENESS_MAX_PAYLOAD_BYTES = 15 * 1024 * 1024; // 15 MB for frame sequence

    private final RestClient restClient;
    private final OcrProperties properties;

    public OcrServiceClient(RestClient ocrRestClient, OcrProperties properties) {
        this.restClient = ocrRestClient;
        this.properties = properties;
    }

    // =========================================================================
    // 1. OCR Extraction Operations
    // =========================================================================

    /**
     * Sends a CCCD card image to the OCR extraction endpoint.
     * Endpoint: POST /api/v1/ocr
     *
     * @param imageBytes The raw image bytes of the CCCD card.
     * @param filename   Original filename (sanitized before sending).
     * @param side       Card side (FRONT, BACK, or AUTO). Defaults to AUTO if null.
     * @param preprocess Whether to enable image enhancement before OCR inference.
     * @return OcrExtractionResponseDto containing extracted fields and OCR metadata.
     * @throws IllegalArgumentException      if image bytes are null, empty, or exceed 10 MB limit.
     * @throws OcrConnectionException        if connection cannot be established.
     * @throws OcrTimeoutException           if the request times out.
     * @throws OcrServerException            if OCR service responds with non-2xx status.
     * @throws OcrInvalidResponseException   if the response body is malformed or unparseable.
     */
    public OcrExtractionResponseDto extractCard(byte[] imageBytes, String filename, CardSide side, Boolean preprocess) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Card image bytes cannot be null or empty");
        }
        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Card image size exceeds maximum allowed limit of 10 MB");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(imageBytes, filename));

        CardSide effectiveSide = side != null ? side : CardSide.AUTO;
        body.add("side", effectiveSide.name());
        body.add("preprocess", String.valueOf(!Boolean.FALSE.equals(preprocess)));

        return execute("POST /api/v1/ocr", () ->
                restClient.post()
                        .uri("/api/v1/ocr")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(OcrExtractionResponseDto.class)
        );
    }

    public OcrExtractionResponseDto extractCard(byte[] imageBytes, String filename, CardSide side) {
        return extractCard(imageBytes, filename, side, true);
    }

    public OcrExtractionResponseDto extractCard(byte[] imageBytes, String filename) {
        return extractCard(imageBytes, filename, CardSide.AUTO, true);
    }

    // =========================================================================
    // 2. Face Verification Operations
    // =========================================================================

    /**
     * Compares a CCCD card portrait with a webcam selfie frame (1:1 biometric matching).
     * Endpoint: POST /api/v1/face/verify
     *
     * IMPORTANT SECURITY INVARIANT:
     * Callers CANNOT specify an arbitrary threshold. The threshold is strictly retrieved
     * from approved server configuration (properties.getFaceMatchThreshold()).
     *
     * @param cardImageBytes   Image bytes of the CCCD card front.
     * @param cardFilename     Filename of the card image.
     * @param selfieImageBytes Image bytes of the selfie frame.
     * @param selfieFilename   Filename of the selfie frame.
     * @return FaceVerificationResponseDto containing match result, similarity score, and face quality checks.
     * @throws IllegalArgumentException      if either image is null, empty, or exceeds 10 MB limit.
     * @throws OcrConnectionException        if connection cannot be established.
     * @throws OcrTimeoutException           if the request times out.
     * @throws OcrServerException            if OCR service responds with non-2xx status (e.g. 503 if models not ready).
     * @throws OcrInvalidResponseException   if the response body is malformed or unparseable.
     */
    public FaceVerificationResponseDto verifyFace(byte[] cardImageBytes, String cardFilename,
                                                 byte[] selfieImageBytes, String selfieFilename) {
        if (cardImageBytes == null || cardImageBytes.length == 0) {
            throw new IllegalArgumentException("Card image bytes cannot be null or empty");
        }
        if (selfieImageBytes == null || selfieImageBytes.length == 0) {
            throw new IllegalArgumentException("Selfie image bytes cannot be null or empty");
        }
        if (cardImageBytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Card image size exceeds maximum allowed limit of 10 MB");
        }
        if (selfieImageBytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Selfie image size exceeds maximum allowed limit of 10 MB");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("card_file", new NamedByteArrayResource(cardImageBytes, cardFilename));
        body.add("selfie_file", new NamedByteArrayResource(selfieImageBytes, selfieFilename));

        if (properties.getFaceMatchThreshold() != null) {
            body.add("threshold", String.valueOf(properties.getFaceMatchThreshold()));
        }

        return execute("POST /api/v1/face/verify", () ->
                restClient.post()
                        .uri("/api/v1/face/verify")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(FaceVerificationResponseDto.class)
        );
    }

    // =========================================================================
    // 3. Active Liveness & Passive Anti-Spoofing Operations
    // =========================================================================

    /**
     * Creates a new randomized active liveness challenge session.
     * Endpoint: POST /api/v1/liveness/session/create
     *
     * @return LivenessSessionCreateResponseDto containing sessionId, expiry seconds, and the initial challenge.
     * @throws OcrConnectionException        if connection cannot be established.
     * @throws OcrTimeoutException           if the request times out.
     * @throws OcrServerException            if OCR service responds with non-2xx status.
     * @throws OcrInvalidResponseException   if the response body is malformed or unparseable.
     */
    public LivenessSessionCreateResponseDto createLivenessSession() {
        return execute("POST /api/v1/liveness/session/create", () ->
                restClient.post()
                        .uri("/api/v1/liveness/session/create")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(LivenessSessionCreateResponseDto.class)
        );
    }

    /**
     * Retrieves the current state, progress, and history of a liveness session.
     * Endpoint: GET /api/v1/liveness/session/{sessionId}
     *
     * @param sessionId The active liveness session ID.
     * @return LivenessSessionStatusResponseDto containing status, current step, passed flag, and history.
     * @throws IllegalArgumentException      if sessionId is null or blank.
     * @throws OcrServerException            if session is not found / expired (HTTP 404).
     */
    public LivenessSessionStatusResponseDto getLivenessSessionStatus(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }

        return execute("GET /api/v1/liveness/session/" + sessionId, () ->
                restClient.get()
                        .uri("/api/v1/liveness/session/{sessionId}", sessionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(LivenessSessionStatusResponseDto.class)
        );
    }

    /**
     * Submits a sequence of webcam frames to verify an active challenge step and evaluate passive PAD.
     * Endpoint: POST /api/v1/liveness/session/{sessionId}/verify-step
     *
     * NOTE: Passive Anti-Spoofing (MiniFASNet dual-scale) is integrated directly into this operation.
     *
     * @param sessionId      The active liveness session ID.
     * @param frameBytesList List of raw JPEG/PNG frame bytes (must be between 3 and 15 frames).
     * @return LivenessStepVerifyResponseDto containing stepPassed, allPassed, antiSpoofingOutcome, and nextChallenge.
     * @throws IllegalArgumentException      if frame count < 3 or > 15, or total payload > 15 MB.
     * @throws OcrServerException            if session not found (404), invalid payload (400), or too large (413).
     */
    public LivenessStepVerifyResponseDto verifyLivenessStep(String sessionId, List<byte[]> frameBytesList) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }
        if (frameBytesList == null || frameBytesList.size() < LIVENESS_MIN_FRAMES) {
            throw new IllegalArgumentException(
                    "Insufficient frames: at least " + LIVENESS_MIN_FRAMES + " consecutive frames required"
            );
        }
        if (frameBytesList.size() > LIVENESS_MAX_FRAMES) {
            throw new IllegalArgumentException(
                    "Too many frames: maximum " + LIVENESS_MAX_FRAMES + " frames allowed per step"
            );
        }

        long totalBytes = 0;
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < frameBytesList.size(); i++) {
            byte[] frame = frameBytesList.get(i);
            if (frame == null || frame.length == 0) {
                throw new IllegalArgumentException("Frame at index " + i + " cannot be null or empty");
            }
            totalBytes += frame.length;
            String filename = "frame_" + i + ".jpg";
            body.add("frames", new NamedByteArrayResource(frame, filename));
        }

        if (totalBytes > LIVENESS_MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Total frame sequence payload exceeds limit of 15 MB");
        }

        return execute("POST /api/v1/liveness/session/" + sessionId + "/verify-step", () ->
                restClient.post()
                        .uri("/api/v1/liveness/session/{sessionId}/verify-step", sessionId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(LivenessStepVerifyResponseDto.class)
        );
    }

    // =========================================================================
    // 4. Preflight Health Check Operations
    // =========================================================================

    /**
     * Checks the health of the PaddleOCR text extraction engine.
     * Endpoint: GET /api/v1/health
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

    // =========================================================================
    // 5. Safe Execution Wrapper & Helpers
    // =========================================================================

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
        } catch (IllegalArgumentException ex) {
            // Re-throw client-side argument validation errors directly
            throw ex;
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

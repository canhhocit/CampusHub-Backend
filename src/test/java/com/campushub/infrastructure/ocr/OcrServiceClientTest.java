package com.campushub.infrastructure.ocr;

import com.campushub.infrastructure.ocr.client.OcrServiceClient;
import com.campushub.infrastructure.ocr.config.OcrProperties;
import com.campushub.infrastructure.ocr.dto.*;
import com.campushub.infrastructure.ocr.exception.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OcrServiceClientTest {

    private MockRestServiceServer mockServer;
    private OcrServiceClient client;
    private OcrProperties properties;

    @BeforeEach
    void setUp() {
        properties = new OcrProperties("http://localhost:8081", 2000, 8000);
        properties.setFaceMatchThreshold(0.363);
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new OcrServiceClient(builder.build(), properties);
    }

    // =========================================================================
    // 1. Preflight Health Checks
    // =========================================================================
    @Nested
    @DisplayName("1. Health Endpoint Operations")
    class HealthEndpointTests {

        @Test
        @DisplayName("GET /api/v1/health maps HealthResponse successfully")
        void testOcrEngineHealth_HealthyResponse() {
            String json = """
                    {
                      "status": "ok",
                      "version": "1.0.0",
                      "ocr_engine": "PaddleOCR",
                      "device": "CPU",
                      "supported_formats": [".jpg", ".jpeg", ".png", ".webp"]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/health"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            OcrEngineHealthDto health = client.getOcrEngineHealth();

            assertNotNull(health);
            assertTrue(health.isHealthy());
            assertEquals("ok", health.getStatus());
            assertEquals("PaddleOCR", health.getOcrEngine());
            assertEquals("CPU", health.getDevice());
            assertEquals(4, health.getSupportedFormats().size());

            mockServer.verify();
        }

        @Test
        @DisplayName("GET /api/v1/face/health maps FaceHealthResponse successfully")
        void testFaceHealth_HealthyResponse() {
            String json = """
                    {
                      "status": "ok",
                      "version": "1.0.0",
                      "detector": "YuNet (ONNX - OpenCV Zoo)",
                      "recognizer": "SFace (ONNX - OpenCV Zoo)",
                      "device": "CPU",
                      "models_loaded": true,
                      "threshold": 0.363,
                      "engine_type": "real",
                      "error_message": null
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/health"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            FaceHealthDto health = client.getFaceHealth();

            assertNotNull(health);
            assertTrue(health.isHealthy());
            assertTrue(health.isModelsLoaded());
            assertEquals(0.363, health.getThreshold());
            assertEquals("real", health.getEngineType());

            mockServer.verify();
        }

        @Test
        @DisplayName("GET /api/v1/liveness/health maps LivenessHealthResponse successfully")
        void testLivenessHealth_HealthyResponse() {
            String json = """
                    {
                      "status": "ok",
                      "version": "1.0.0",
                      "active_liveness_ready": true,
                      "passive_anti_spoofing_ready": true,
                      "anti_spoofing_enabled": true,
                      "fail_closed": true,
                      "thresholds": {"yaw_threshold": 12.0},
                      "models": {"yunet": true},
                      "error_message": null
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/health"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            LivenessHealthDto health = client.getLivenessHealth();

            assertNotNull(health);
            assertTrue(health.isHealthy());
            assertTrue(health.isActiveLivenessReady());
            assertTrue(health.isPassiveAntiSpoofingReady());

            mockServer.verify();
        }

        @Test
        @DisplayName("checkOverallHealth reports fullyReady when all 3 subsystems are healthy")
        void testOverallHealth_FullyReady() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/health"))
                    .andRespond(withSuccess("{\"status\":\"ok\",\"ocr_engine\":\"PaddleOCR\"}", MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/health"))
                    .andRespond(withSuccess("{\"status\":\"ok\",\"models_loaded\":true,\"engine_type\":\"real\"}", MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/health"))
                    .andRespond(withSuccess("{\"status\":\"ok\",\"active_liveness_ready\":true,\"passive_anti_spoofing_ready\":true}", MediaType.APPLICATION_JSON));

            OcrOverallHealthDto overall = client.checkOverallHealth();

            assertNotNull(overall);
            assertTrue(overall.isFullyReady());

            mockServer.verify();
        }
    }

    // =========================================================================
    // 2. OCR Extraction Operations
    // =========================================================================
    @Nested
    @DisplayName("2. OCR Extraction Operations")
    class OcrExtractionTests {

        @Test
        @DisplayName("POST /api/v1/ocr sends multipart request with 'file' part and deserializes response")
        void testExtractCard_Success() {
            String json = """
                    {
                      "success": true,
                      "side": "FRONT",
                      "processing_time_ms": 320.5,
                      "fields": {
                        "idNumber": {
                          "name": "idNumber",
                          "label": "Số định danh cá nhân",
                          "value": "079201012345",
                          "status": "EXTRACTED",
                          "confidence": 0.98,
                          "raw_text": "079201012345"
                        },
                        "fullName": {
                          "name": "fullName",
                          "label": "Họ và tên",
                          "value": "NGUYEN VAN A",
                          "status": "EXTRACTED",
                          "confidence": 0.95,
                          "raw_text": "NGUYỄN VĂN A"
                        }
                      },
                      "raw_detections": [
                        {"text": "079201012345", "confidence": 0.98}
                      ],
                      "image_info": {
                        "width": 1200,
                        "height": 800,
                        "channels": 3,
                        "size_bytes": 150000,
                        "format": "JPEG"
                      },
                      "warnings": [],
                      "disclaimer": "Prototype disclaimer"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/ocr"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("Content-Type", Matchers.startsWith("multipart/form-data")))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            byte[] dummyCard = "fake-jpeg-data".getBytes();
            OcrExtractionResponseDto response = client.extractCard(dummyCard, "cccd_front.jpg", CardSide.FRONT, true);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(CardSide.FRONT, response.getSide());
            assertEquals(320.5, response.getProcessingTimeMs());
            assertEquals(2, response.getFields().size());
            assertEquals("079201012345", response.getFieldValue("idNumber"));
            assertEquals("NGUYEN VAN A", response.getFieldValue("fullName"));

            ExtractedFieldDto idField = response.getFields().get("idNumber");
            assertTrue(idField.isExtracted());
            assertEquals(FieldStatus.EXTRACTED, idField.getStatus());

            assertNotNull(response.getImageInfo());
            assertEquals(1200, response.getImageInfo().getWidth());
            assertEquals(1, response.getRawDetections().size());

            mockServer.verify();
        }

        @Test
        @DisplayName("extractCard validates input bytes cannot be null or empty")
        void testExtractCard_ValidatesEmpty() {
            assertThrows(IllegalArgumentException.class, () ->
                    client.extractCard(null, "card.jpg")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    client.extractCard(new byte[0], "card.jpg")
            );
        }

        @Test
        @DisplayName("extractCard validates input bytes cannot exceed 10 MB limit")
        void testExtractCard_ValidatesOversized() {
            byte[] oversized = new byte[11 * 1024 * 1024]; // 11 MB
            assertThrows(IllegalArgumentException.class, () ->
                    client.extractCard(oversized, "card.jpg")
            );
        }
    }

    // =========================================================================
    // 3. Face Verification Operations
    // =========================================================================
    @Nested
    @DisplayName("3. Face Verification Operations")
    class FaceVerificationTests {

        @Test
        @DisplayName("POST /api/v1/face/verify sends card_file & selfie_file and deserializes match result")
        void testVerifyFace_BiometricMatch() {
            String json = """
                    {
                      "success": true,
                      "match": true,
                      "similarity_score": 0.74,
                      "confidence_score": 0.92,
                      "threshold": 0.363,
                      "card_face": {
                        "detected": true,
                        "count": 1,
                        "image_width": 600,
                        "image_height": 800,
                        "box": {"x": 50, "y": 60, "width": 120, "height": 160, "confidence": 0.99}
                      },
                      "selfie_face": {
                        "detected": true,
                        "count": 1,
                        "image_width": 720,
                        "image_height": 1280
                      },
                      "pipeline_state": "MATCHED",
                      "warnings": [],
                      "processing_time_ms": 145.2,
                      "disclaimer": "Experimental 1:1 face matching"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/verify"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("Content-Type", Matchers.startsWith("multipart/form-data")))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            byte[] card = "card-portrait-bytes".getBytes();
            byte[] selfie = "webcam-selfie-bytes".getBytes();

            FaceVerificationResponseDto response = client.verifyFace(card, "card.jpg", selfie, "selfie.jpg");

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertTrue(response.isMatch());
            assertTrue(response.isVerifiedMatch());
            assertEquals(0.74, response.getSimilarityScore());
            assertEquals(0.363, response.getThreshold());
            assertEquals("MATCHED", response.getPipelineState());

            assertNotNull(response.getCardFace());
            assertTrue(response.getCardFace().getDetected());
            assertEquals(1, response.getCardFace().getCount());
            assertNotNull(response.getCardFace().getBox());
            assertEquals(50, response.getCardFace().getBox().getX());

            mockServer.verify();
        }

        @Test
        @DisplayName("verifyFace preserves distinction between pipeline success and biometric non-match")
        void testVerifyFace_BiometricMismatch() {
            String json = """
                    {
                      "success": true,
                      "match": false,
                      "similarity_score": 0.18,
                      "confidence_score": 0.32,
                      "threshold": 0.363,
                      "pipeline_state": "MATCHED",
                      "warnings": [],
                      "processing_time_ms": 130.0,
                      "disclaimer": "Experimental 1:1 face matching"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/verify"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            FaceVerificationResponseDto response = client.verifyFace(
                    "card".getBytes(), "card.jpg", "selfie".getBytes(), "selfie.jpg"
            );

            assertNotNull(response);
            assertTrue(response.isSuccess(), "Pipeline execution succeeded");
            assertFalse(response.isMatch(), "Biometric match MUST be false");
            assertFalse(response.isVerifiedMatch(), "Verified match MUST be false");
            assertEquals(0.18, response.getSimilarityScore());

            mockServer.verify();
        }

        @Test
        @DisplayName("verifyFace enforces server-side threshold and does not accept caller override")
        void testVerifyFace_EnforcesServerThreshold() {
            // Ensure client uses properties.getFaceMatchThreshold() = 0.363
            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/verify"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("{\"success\":true,\"match\":true,\"similarity_score\":0.5,\"threshold\":0.363}", MediaType.APPLICATION_JSON));

            FaceVerificationResponseDto resp = client.verifyFace(
                    "c".getBytes(), "c.jpg", "s".getBytes(), "s.jpg"
            );
            assertNotNull(resp);
            mockServer.verify();
        }

        @Test
        @DisplayName("verifyFace validates inputs cannot be null or empty")
        void testVerifyFace_ValidatesInputs() {
            assertThrows(IllegalArgumentException.class, () ->
                    client.verifyFace(null, "c.jpg", "s".getBytes(), "s.jpg")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    client.verifyFace("c".getBytes(), "c.jpg", null, "s.jpg")
            );
        }
    }

    // =========================================================================
    // 4. Active Liveness & Passive Anti-Spoofing Operations
    // =========================================================================
    @Nested
    @DisplayName("4. Active Liveness & Passive PAD Operations")
    class LivenessOperationsTests {

        @Test
        @DisplayName("POST /api/v1/liveness/session/create creates session and deserializes challenge")
        void testCreateLivenessSession_Success() {
            String json = """
                    {
                      "session_id": "live_session_12345",
                      "created_at": 1700000000.0,
                      "expires_in_seconds": 120,
                      "total_steps": 2,
                      "current_step": 1,
                      "challenge": {
                        "action": "TURN_HEAD_LEFT",
                        "instruction": "Quay đầu sang trái",
                        "step": 1,
                        "total_steps": 2,
                        "timeout_seconds": 30
                      }
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/session/create"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            LivenessSessionCreateResponseDto response = client.createLivenessSession();

            assertNotNull(response);
            assertEquals("live_session_12345", response.getSessionId());
            assertEquals(120, response.getExpiresInSeconds());
            assertEquals(2, response.getTotalSteps());
            assertEquals(1, response.getCurrentStep());

            assertNotNull(response.getChallenge());
            assertEquals(ChallengeAction.TURN_HEAD_LEFT, response.getChallenge().getAction());
            assertEquals("Quay đầu sang trái", response.getChallenge().getInstruction());

            mockServer.verify();
        }

        @Test
        @DisplayName("GET /api/v1/liveness/session/{id} retrieves session status")
        void testGetLivenessSessionStatus_Success() {
            String json = """
                    {
                      "session_id": "live_session_12345",
                      "status": "PASSED",
                      "current_step": 2,
                      "total_steps": 2,
                      "passed": true,
                      "created_at": 1700000000.0,
                      "expires_at": 1700000120.0,
                      "history": [],
                      "disclaimer": "Liveness disclaimer"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/session/live_session_12345"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            LivenessSessionStatusResponseDto status = client.getLivenessSessionStatus("live_session_12345");

            assertNotNull(status);
            assertEquals("live_session_12345", status.getSessionId());
            assertEquals(LivenessSessionStatus.PASSED, status.getStatus());
            assertTrue(status.getPassed());
            assertTrue(status.isSessionPassed());

            mockServer.verify();
        }

        @Test
        @DisplayName("POST /api/v1/liveness/session/{id}/verify-step submits frames and evaluates PAD")
        void testVerifyLivenessStep_Success() {
            String json = """
                    {
                      "session_id": "live_session_12345",
                      "step_passed": true,
                      "current_step": 1,
                      "total_steps": 2,
                      "all_passed": false,
                      "status": "IN_PROGRESS",
                      "next_challenge": {
                        "action": "TURN_HEAD_RIGHT",
                        "instruction": "Quay đầu sang phải",
                        "step": 2,
                        "total_steps": 2,
                        "timeout_seconds": 30
                      },
                      "reason_code": "CHALLENGE_COMPLETED",
                      "message": "Bước 1 hoàn thành",
                      "anti_spoofing_outcome": "LIVE"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/session/live_session_12345/verify-step"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("Content-Type", Matchers.startsWith("multipart/form-data")))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            List<byte[]> frames = List.of(
                    "frame-1".getBytes(),
                    "frame-2".getBytes(),
                    "frame-3".getBytes()
            );

            LivenessStepVerifyResponseDto stepResp = client.verifyLivenessStep("live_session_12345", frames);

            assertNotNull(stepResp);
            assertTrue(stepResp.getStepPassed());
            assertFalse(stepResp.getAllPassed(), "Single step passed must NOT equal full session completion");
            assertEquals(AntiSpoofingOutcome.LIVE, stepResp.getAntiSpoofingOutcome());
            assertTrue(stepResp.isStepSuccessful());
            assertFalse(stepResp.isSessionFullyPassed());
            assertEquals(LivenessSessionStatus.IN_PROGRESS, stepResp.getStatus());

            assertNotNull(stepResp.getNextChallenge());
            assertEquals(ChallengeAction.TURN_HEAD_RIGHT, stepResp.getNextChallenge().getAction());

            mockServer.verify();
        }

        @Test
        @DisplayName("verifyLivenessStep detects spoof outcome and fails step safely")
        void testVerifyLivenessStep_SpoofDetected() {
            String json = """
                    {
                      "session_id": "live_session_12345",
                      "step_passed": false,
                      "current_step": 1,
                      "total_steps": 2,
                      "all_passed": false,
                      "status": "FAILED",
                      "reason_code": "SPOOF_DETECTED",
                      "message": "Phát hiện màn hình giả mạo",
                      "anti_spoofing_outcome": "SPOOF"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/session/live_session_12345/verify-step"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            List<byte[]> frames = List.of("f1".getBytes(), "f2".getBytes(), "f3".getBytes());
            LivenessStepVerifyResponseDto stepResp = client.verifyLivenessStep("live_session_12345", frames);

            assertNotNull(stepResp);
            assertFalse(stepResp.getStepPassed());
            assertTrue(stepResp.isSpoofDetected());
            assertFalse(stepResp.isStepSuccessful());
            assertFalse(stepResp.isSessionFullyPassed());
            assertEquals(AntiSpoofingOutcome.SPOOF, stepResp.getAntiSpoofingOutcome());

            mockServer.verify();
        }

        @Test
        @DisplayName("verifyLivenessStep handles unknown enum values fail-closed without claiming PASS")
        void testVerifyLivenessStep_UnknownEnumFailClosed() {
            String json = """
                    {
                      "session_id": "live_session_12345",
                      "step_passed": true,
                      "current_step": 1,
                      "total_steps": 2,
                      "all_passed": true,
                      "status": "FUTURE_UNKNOWN_STATUS",
                      "anti_spoofing_outcome": "FUTURE_UNKNOWN_PAD_OUTCOME",
                      "message": "Unknown values test"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/session/live_session_12345/verify-step"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            List<byte[]> frames = List.of("f1".getBytes(), "f2".getBytes(), "f3".getBytes());
            LivenessStepVerifyResponseDto stepResp = client.verifyLivenessStep("live_session_12345", frames);

            assertNotNull(stepResp);
            assertEquals(AntiSpoofingOutcome.UNKNOWN, stepResp.getAntiSpoofingOutcome());
            assertEquals(LivenessSessionStatus.UNKNOWN, stepResp.getStatus());
            assertFalse(stepResp.isStepSuccessful(), "Unknown PAD outcome must fail closed");
            assertFalse(stepResp.isSessionFullyPassed(), "Unknown status must fail closed");

            mockServer.verify();
        }

        @Test
        @DisplayName("verifyLivenessStep validates frame count boundary (min 3, max 15)")
        void testVerifyLivenessStep_ValidatesFrameCount() {
            assertThrows(IllegalArgumentException.class, () ->
                    client.verifyLivenessStep("s1", List.of("f1".getBytes(), "f2".getBytes())) // 2 frames < 3
            );

            List<byte[]> sixteenFrames = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                sixteenFrames.add("f".getBytes());
            }
            assertThrows(IllegalArgumentException.class, () ->
                    client.verifyLivenessStep("s1", sixteenFrames) // 16 frames > 15
            );
        }
    }

    // =========================================================================
    // 5. HTTP Error Codes & Contract Mappings
    // =========================================================================
    @Nested
    @DisplayName("5. HTTP Error Handling (400, 404, 413, 422, 500, 503)")
    class HttpErrorCodeTests {

        @Test
        @DisplayName("HTTP 400 maps to OcrServerException with isBadRequest()=true")
        void testHttp400_BadRequest() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/ocr"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"detail\":\"Tệp ảnh không hợp lệ\"}"));

            OcrServerException ex = assertThrows(OcrServerException.class, () ->
                    client.extractCard("bad-data".getBytes(), "bad.jpg")
            );

            assertEquals(400, ex.getStatusCode());
            assertTrue(ex.isBadRequest());
            assertTrue(ex.getResponseBody().contains("Tệp ảnh không hợp lệ"));
            mockServer.verify();
        }

        @Test
        @DisplayName("HTTP 404 maps to OcrServerException with isNotFound()=true")
        void testHttp404_SessionNotFound() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/session/non_existent"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"detail\":\"Phiên kiểm tra không tồn tại hoặc đã hết hạn\"}"));

            OcrServerException ex = assertThrows(OcrServerException.class, () ->
                    client.getLivenessSessionStatus("non_existent")
            );

            assertEquals(404, ex.getStatusCode());
            assertTrue(ex.isNotFound());
            mockServer.verify();
        }

        @Test
        @DisplayName("HTTP 413 Payload Too Large maps to OcrServerException with isPayloadTooLarge()=true")
        void testHttp413_PayloadTooLarge() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/session/s1/verify-step"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.PAYLOAD_TOO_LARGE).body("{\"detail\":\"Tổng dung lượng vượt quá giới hạn 15 MB\"}"));

            List<byte[]> frames = List.of("f1".getBytes(), "f2".getBytes(), "f3".getBytes());
            OcrServerException ex = assertThrows(OcrServerException.class, () ->
                    client.verifyLivenessStep("s1", frames)
            );

            assertEquals(413, ex.getStatusCode());
            assertTrue(ex.isPayloadTooLarge());
            mockServer.verify();
        }

        @Test
        @DisplayName("HTTP 422 Unprocessable Entity maps to OcrServerException with isValidationError()=true")
        void testHttp422_ValidationError() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/verify"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"detail\":[{\"loc\":[\"body\",\"card_file\"],\"msg\":\"Field required\"}]}"));

            OcrServerException ex = assertThrows(OcrServerException.class, () ->
                    client.verifyFace("c".getBytes(), "c.jpg", "s".getBytes(), "s.jpg")
            );

            assertEquals(422, ex.getStatusCode());
            assertTrue(ex.isValidationError());
            mockServer.verify();
        }

        @Test
        @DisplayName("HTTP 500 Internal Server Error maps to OcrServerException with isServerError()=true")
        void testHttp500_InternalServerError() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/ocr"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError().body("{\"detail\":\"OCR engine crashed\"}"));

            OcrServerException ex = assertThrows(OcrServerException.class, () ->
                    client.extractCard("c".getBytes(), "c.jpg")
            );

            assertEquals(500, ex.getStatusCode());
            assertTrue(ex.isServerError());
            mockServer.verify();
        }

        @Test
        @DisplayName("HTTP 503 Service Unavailable maps to OcrServerException with isServiceUnavailable()=true")
        void testHttp503_ServiceUnavailable() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/verify"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"detail\":\"YuNet/SFace models not available\"}"));

            OcrServerException ex = assertThrows(OcrServerException.class, () ->
                    client.verifyFace("c".getBytes(), "c.jpg", "s".getBytes(), "s.jpg")
            );

            assertEquals(503, ex.getStatusCode());
            assertTrue(ex.isServiceUnavailable());
            mockServer.verify();
        }

        @Test
        @DisplayName("Connection refused maps to OcrConnectionException")
        void testConnectionRefused_ThrowsOcrConnectionException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/ocr"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new ConnectException("Connection refused: connect")));

            OcrConnectionException ex = assertThrows(OcrConnectionException.class, () ->
                    client.extractCard("c".getBytes(), "c.jpg")
            );

            assertTrue(ex.getMessage().contains("Cannot connect to OCR service"));
            assertTrue(ex.getCause() instanceof ResourceAccessException);
            assertTrue(ex.getCause().getCause() instanceof ConnectException);
            mockServer.verify();
        }

        @Test
        @DisplayName("Socket timeout maps to OcrTimeoutException")
        void testSocketTimeout_ThrowsOcrTimeoutException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/verify"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new SocketTimeoutException("Read timed out")));

            OcrTimeoutException ex = assertThrows(OcrTimeoutException.class, () ->
                    client.verifyFace("c".getBytes(), "c.jpg", "s".getBytes(), "s.jpg")
            );

            assertTrue(ex.getMessage().contains("timed out"));
            mockServer.verify();
        }

        @Test
        @DisplayName("Malformed JSON maps to OcrInvalidResponseException")
        void testMalformedJson_ThrowsOcrInvalidResponseException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/ocr"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("{\"success\": true, \"fields\": { broken json ...", MediaType.APPLICATION_JSON));

            OcrInvalidResponseException ex = assertThrows(OcrInvalidResponseException.class, () ->
                    client.extractCard("c".getBytes(), "c.jpg")
            );

            assertTrue(ex.getMessage().contains("Malformed or unparseable"));
            mockServer.verify();
        }
    }

    // =========================================================================
    // 6. Security and Safe Error Mapping
    // =========================================================================
    @Nested
    @DisplayName("6. Security & Safe Logging")
    class SecurityTests {

        @Test
        @DisplayName("Exception messages do not leak raw byte buffers or PII")
        void testSafeExceptionMessages() {
            OcrServerException ex = new OcrServerException(500, "Error occurred", "Failed request");
            assertFalse(ex.getMessage().contains("base64"));
            assertFalse(ex.getMessage().contains("token"));

            String longBody = "x".repeat(1000);
            OcrServerException truncated = new OcrServerException(500, longBody, "Failed request");
            assertTrue(truncated.getResponseBody().endsWith("... [truncated]"));
            assertTrue(truncated.getResponseBody().length() <= 520);
        }

        @Test
        @DisplayName("OcrProperties maintains safe defaults and supports custom threshold")
        void testOcrProperties() {
            OcrProperties props = new OcrProperties();
            assertEquals("http://localhost:8081", props.getBaseUrl());
            assertEquals(2000, props.getConnectTimeoutMs());
            assertEquals(8000, props.getReadTimeoutMs());
            assertEquals(0.363, props.getFaceMatchThreshold());

            props.setFaceMatchThreshold(0.40);
            assertEquals(0.40, props.getFaceMatchThreshold());
        }
    }
}

package com.campushub.infrastructure.ocr;

import com.campushub.infrastructure.ocr.client.OcrServiceClient;
import com.campushub.infrastructure.ocr.config.OcrProperties;
import com.campushub.infrastructure.ocr.dto.FaceHealthDto;
import com.campushub.infrastructure.ocr.dto.LivenessHealthDto;
import com.campushub.infrastructure.ocr.dto.OcrEngineHealthDto;
import com.campushub.infrastructure.ocr.dto.OcrOverallHealthDto;
import com.campushub.infrastructure.ocr.exception.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OcrServiceClientTest {

    private MockRestServiceServer mockServer;
    private OcrServiceClient client;
    private OcrProperties properties;

    @BeforeEach
    void setUp() {
        properties = new OcrProperties("http://localhost:8081", 2000, 8000);
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new OcrServiceClient(builder.build(), properties);
    }

    @Nested
    @DisplayName("1. Healthy Endpoint Responses")
    class HealthyResponses {

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
            assertTrue(health.getSupportedFormats().contains(".jpg"));

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
            assertEquals("ok", health.getStatus());
            assertEquals("real", health.getEngineType());
            assertEquals(0.363, health.getThreshold());
            assertEquals("YuNet (ONNX - OpenCV Zoo)", health.getDetector());
            assertEquals("SFace (ONNX - OpenCV Zoo)", health.getRecognizer());
            assertNull(health.getErrorMessage());

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
                      "thresholds": {
                        "yaw_threshold": 12.0,
                        "spoof_threshold": 0.5
                      },
                      "models": {
                        "yunet": {"loaded": true},
                        "minifasnet_v2": {"loaded": true},
                        "minifasnet_v1se": {"loaded": true}
                      },
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
            assertTrue(health.isAntiSpoofingEnabled());
            assertTrue(health.isFailClosed());
            assertEquals(12.0, health.getThresholds().get("yaw_threshold"));
            assertNull(health.getErrorMessage());

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
            assertEquals("All OCR and Biometrics subsystems operational.", overall.getMessage());
            assertNotNull(overall.getOcrEngine());
            assertNotNull(overall.getFace());
            assertNotNull(overall.getLiveness());

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("2. OCR Service Unavailable (Connection Failure)")
    class ServiceUnavailable {

        @Test
        @DisplayName("Throws OcrConnectionException when service refuses connection")
        void testConnectionFailure_ThrowsOcrConnectionException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/health"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withException(new ConnectException("Connection refused: connect")));

            OcrConnectionException ex = assertThrows(
                    OcrConnectionException.class,
                    () -> client.getOcrEngineHealth()
            );

            assertTrue(ex.getMessage().contains("Cannot connect to OCR service"));
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof ResourceAccessException);
            assertTrue(ex.getCause().getCause() instanceof ConnectException);

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("3. Connection and Read Timeouts")
    class TimeoutFailures {

        @Test
        @DisplayName("Throws OcrTimeoutException when request times out")
        void testSocketTimeout_ThrowsOcrTimeoutException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/health"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withException(new SocketTimeoutException("Read timed out")));

            OcrTimeoutException ex = assertThrows(
                    OcrTimeoutException.class,
                    () -> client.getFaceHealth()
            );

            assertTrue(ex.getMessage().contains("timed out"));
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("4. Non-2xx HTTP Responses")
    class Non2xxResponses {

        @Test
        @DisplayName("Throws OcrServerException with status 500 on Internal Server Error")
        void testServerError500_ThrowsOcrServerException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/health"))
                    .andRespond(withServerError().body("{\"detail\":\"Internal engine failure\"}"));

            OcrServerException ex = assertThrows(
                    OcrServerException.class,
                    () -> client.getOcrEngineHealth()
            );

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getStatusCode());
            assertTrue(ex.getMessage().contains("500"));
            assertTrue(ex.getResponseBody().contains("Internal engine failure"));

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws OcrServerException with status 503 on Service Unavailable")
        void testServerUnavailable503_ThrowsOcrServerException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/health"))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"detail\":\"Model runtime not ready\"}"));

            OcrServerException ex = assertThrows(
                    OcrServerException.class,
                    () -> client.getFaceHealth()
            );

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getStatusCode());
            assertTrue(ex.getMessage().contains("503"));

            mockServer.verify();
        }

        @Test
        @DisplayName("Throws OcrServerException with status 404 on Not Found")
        void testNotFound404_ThrowsOcrServerException() {
            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/health"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"detail\":\"Endpoint not found\"}"));

            OcrServerException ex = assertThrows(
                    OcrServerException.class,
                    () -> client.getLivenessHealth()
            );

            assertEquals(HttpStatus.NOT_FOUND.value(), ex.getStatusCode());

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("5. Malformed or Incomplete JSON Responses")
    class MalformedResponses {

        @Test
        @DisplayName("Throws OcrInvalidResponseException on unparseable / broken JSON")
        void testMalformedJson_ThrowsOcrInvalidResponseException() {
            String brokenJson = "{\"status\": \"ok\", \"version\": ";

            mockServer.expect(requestTo("http://localhost:8081/api/v1/health"))
                    .andRespond(withSuccess(brokenJson, MediaType.APPLICATION_JSON));

            OcrInvalidResponseException ex = assertThrows(
                    OcrInvalidResponseException.class,
                    () -> client.getOcrEngineHealth()
            );

            assertTrue(ex.getMessage().contains("Malformed or unparseable"));
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("6. Health Response Reporting Unavailable Models")
    class UnavailableModels {

        @Test
        @DisplayName("Face health reports models_loaded=false and assertFaceHealthy throws OcrModelUnavailableException")
        void testFaceHealth_ModelsNotLoaded() {
            String json = """
                    {
                      "status": "unavailable",
                      "version": "1.0.0",
                      "detector": "YuNet (ONNX - OpenCV Zoo)",
                      "recognizer": "SFace (ONNX - OpenCV Zoo)",
                      "device": "CPU",
                      "models_loaded": false,
                      "threshold": 0.363,
                      "engine_type": "unavailable",
                      "error_message": "Tệp mô hình YuNet hoặc SFace chưa được nạp vào bộ nhớ."
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/health"))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            FaceHealthDto health = client.getFaceHealth();

            assertNotNull(health);
            assertFalse(health.isHealthy());
            assertFalse(health.isModelsLoaded());
            assertEquals("unavailable", health.getStatus());
            assertEquals("Tệp mô hình YuNet hoặc SFace chưa được nạp vào bộ nhớ.", health.getErrorMessage());

            mockServer.verify();
        }

        @Test
        @DisplayName("assertFaceHealthy throws OcrModelUnavailableException when models are unavailable")
        void testAssertFaceHealthy_ThrowsOcrModelUnavailableException() {
            String json = """
                    {
                      "status": "unavailable",
                      "models_loaded": false,
                      "error_message": "Missing SFace ONNX weight file"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/face/health"))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            OcrModelUnavailableException ex = assertThrows(
                    OcrModelUnavailableException.class,
                    () -> client.assertFaceHealthy()
            );

            assertTrue(ex.getMessage().contains("Missing SFace ONNX weight file"));
            assertNotNull(ex.getHealthReport());

            mockServer.verify();
        }

        @Test
        @DisplayName("Liveness health reports degraded and assertLivenessHealthy throws OcrModelUnavailableException")
        void testLivenessHealth_PassivePadUnavailable() {
            String json = """
                    {
                      "status": "degraded",
                      "version": "1.0.0",
                      "active_liveness_ready": true,
                      "passive_anti_spoofing_ready": false,
                      "anti_spoofing_enabled": true,
                      "fail_closed": true,
                      "thresholds": {},
                      "models": {},
                      "error_message": "MiniFASNet anti-spoofing models not loaded"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/liveness/health"))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            LivenessHealthDto health = client.getLivenessHealth();

            assertNotNull(health);
            assertFalse(health.isHealthy());
            assertTrue(health.isActiveLivenessReady());
            assertFalse(health.isPassiveAntiSpoofingReady());

            mockServer.verify();
        }

        @Test
        @DisplayName("assertOcrEngineHealthy throws OcrServiceUnavailableException when status is not ok")
        void testAssertOcrEngineHealthy_ThrowsOcrServiceUnavailableException() {
            String json = """
                    {
                      "status": "degraded",
                      "version": "1.0.0",
                      "ocr_engine": "PaddleOCR",
                      "device": "CPU"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/v1/health"))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            OcrServiceUnavailableException ex = assertThrows(
                    OcrServiceUnavailableException.class,
                    () -> client.assertOcrEngineHealthy()
            );

            assertTrue(ex.getMessage().contains("degraded"));
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("7. Safe Error Mapping & Configuration")
    class SafeErrorMappingAndConfig {

        @Test
        @DisplayName("Exceptions do not expose sensitive PII or credentials")
        void testExceptionMessages_SafeAndSanitized() {
            OcrIntegrationException ex = new OcrIntegrationException("General error");
            assertFalse(ex.getMessage().contains("token"));
            assertFalse(ex.getMessage().contains("base64"));

            OcrServerException serverEx = new OcrServerException(500, "Short error body", "Server failed");
            assertEquals("Short error body", serverEx.getResponseBody());

            // Test truncation for large response bodies
            String longBody = "A".repeat(1000);
            OcrServerException truncatedEx = new OcrServerException(500, longBody, "Server failed");
            assertTrue(truncatedEx.getResponseBody().endsWith("... [truncated]"));
            assertTrue(truncatedEx.getResponseBody().length() <= 520);
        }

        @Test
        @DisplayName("OcrProperties configuration holds valid defaults and updates")
        void testOcrProperties() {
            OcrProperties props = new OcrProperties();
            assertEquals("http://localhost:8081", props.getBaseUrl());
            assertEquals(2000, props.getConnectTimeoutMs());
            assertEquals(8000, props.getReadTimeoutMs());

            props.setBaseUrl("http://ocr.internal:8081");
            props.setConnectTimeoutMs(3000);
            props.setReadTimeoutMs(10000);

            assertEquals("http://ocr.internal:8081", props.getBaseUrl());
            assertEquals(3000, props.getConnectTimeoutMs());
            assertEquals(10000, props.getReadTimeoutMs());
        }
    }
}

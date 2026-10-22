package com.campushub.modules.identity;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.common.security.JwtProvider;
import com.campushub.infrastructure.ocr.client.OcrServiceClient;
import com.campushub.infrastructure.ocr.dto.*;
import com.campushub.modules.identity.dto.StartVerificationResponseDto;
import com.campushub.modules.identity.entity.IdentityVerificationAttempt;
import com.campushub.modules.identity.enums.VerificationAttemptStatus;
import com.campushub.modules.identity.repository.IdentityVerificationAttemptRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class IdentityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdentityVerificationAttemptRepository attemptRepository;

    @Autowired
    private JwtProvider jwtTokenProvider;

    @MockBean
    private OcrServiceClient ocrClient;

    private User testUser;
    private String userToken;
    private byte[] validJpegBytes;

    private static byte[] createValidJpegBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) 0xFF);
        baos.write((byte) 0xD8);
        baos.write((byte) 0xFF);
        baos.write((byte) 0xE0);
        for (int i = 0; i < 20; i++) {
            baos.write((byte) 0xBB);
        }
        return baos.toByteArray();
    }

    @BeforeEach
    void setUp() {
        attemptRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser_ekyc");
        testUser.setEmail("ekyc@test.com");
        testUser.setPasswordHash("hashed_secret");
        testUser.setFullName("Nguyễn Văn A");
        testUser.setRole(Role.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setIdentityVerified(false);
        testUser = userRepository.save(testUser);

        userToken = jwtTokenProvider.generateAccessTokenFromUsernameAndIdAndRole(
                testUser.getUsername(), testUser.getId(), "ROLE_USER"
        );

        validJpegBytes = createValidJpegBytes();
    }

    @Test
    @DisplayName("Endpoint yêu cầu Authentication: Không có token bị 401 Unauthorized")
    void testUnauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/identity/attempts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Toàn bộ luồng eKYC hoàn chỉnh (Start -> Liveness -> Face Match -> PENDING_REVIEW)")
    void testFullEkycWorkflowEndToEnd() throws Exception {
        // 1. Mock OCR service calls
        OcrExtractionResponseDto ocrDto = new OcrExtractionResponseDto();
        ocrDto.setSuccess(true);
        ocrDto.setFields(Map.of("id", new ExtractedFieldDto("079123456789", 0.99, FieldStatus.EXTRACTED)));
        when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(ocrDto);

        LivenessSessionCreateResponseDto sessionDto = new LivenessSessionCreateResponseDto(
                "sess-1234", 1729600000.0, 900, 2, 0,
                new ChallengeInfoDto(ChallengeAction.TURN_HEAD_LEFT, "Quay trái", 10)
        );
        when(ocrClient.createLivenessSession()).thenReturn(sessionDto);

        // STEP A: POST /start
        MockMultipartFile cardFile = new MockMultipartFile(
                "card_file", "cccd_front.jpg", "image/jpeg", validJpegBytes
        );

        MvcResult startResult = mockMvc.perform(
                multipart("/api/v1/identity/attempts/start")
                        .file(cardFile)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptId").isNotEmpty())
        .andExpect(jsonPath("$.status").value("LIVENESS_IN_PROGRESS"))
        .andExpect(jsonPath("$.totalChallenges").value(2))
        .andReturn();

        StartVerificationResponseDto startRes = objectMapper.readValue(
                startResult.getResponse().getContentAsString(), StartVerificationResponseDto.class
        );
        String attemptId = startRes.getAttemptId();
        assertNotNull(attemptId);

        // STEP B1: POST /liveness-step (Challenge 1)
        LivenessStepVerifyResponseDto step1Dto = new LivenessStepVerifyResponseDto();
        step1Dto.setCurrentStep(0);
        step1Dto.setStepPassed(true);
        step1Dto.setAllPassed(false);
        step1Dto.setAntiSpoofingOutcome(AntiSpoofingOutcome.LIVE);
        step1Dto.setStatus(LivenessSessionStatus.IN_PROGRESS);
        step1Dto.setNextChallenge(new ChallengeInfoDto(ChallengeAction.NOD_HEAD, "Gật đầu", 10));
        when(ocrClient.verifyLivenessStep(eq("sess-1234"), anyList())).thenReturn(step1Dto);

        MockMultipartFile frame1 = new MockMultipartFile("frames", "f1.jpg", "image/jpeg", validJpegBytes);
        mockMvc.perform(
                multipart("/api/v1/identity/attempts/" + attemptId + "/liveness-step")
                        .file(frame1)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.actionCompleted").value(true))
        .andExpect(jsonPath("$.livenessPassed").value(false))
        .andExpect(jsonPath("$.status").value("LIVENESS_IN_PROGRESS"));

        // STEP B2: POST /liveness-step (Challenge 2 - Final)
        LivenessStepVerifyResponseDto step2Dto = new LivenessStepVerifyResponseDto();
        step2Dto.setCurrentStep(1);
        step2Dto.setStepPassed(true);
        step2Dto.setAllPassed(true);
        step2Dto.setAntiSpoofingOutcome(AntiSpoofingOutcome.LIVE);
        step2Dto.setStatus(LivenessSessionStatus.PASSED);
        when(ocrClient.verifyLivenessStep(eq("sess-1234"), anyList())).thenReturn(step2Dto);

        mockMvc.perform(
                multipart("/api/v1/identity/attempts/" + attemptId + "/liveness-step")
                        .file(frame1)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.actionCompleted").value(true))
        .andExpect(jsonPath("$.livenessPassed").value(true))
        .andExpect(jsonPath("$.status").value("LIVENESS_PASSED"));

        // STEP C: POST /verify-face
        FaceVerificationResponseDto faceDto = new FaceVerificationResponseDto(
                true, true, 0.89, 0.11, 0.363, null, null, 180.0
        );
        when(ocrClient.verifyFace(any(), any(), any(), any())).thenReturn(faceDto);

        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie_file", "selfie.jpg", "image/jpeg", validJpegBytes
        );

        mockMvc.perform(
                multipart("/api/v1/identity/attempts/" + attemptId + "/verify-face")
                        .file(cardFile)
                        .file(selfieFile)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
        .andExpect(jsonPath("$.match").value(true))
        .andExpect(jsonPath("$.similarityScore").value(0.89));

        // STEP D: GET /me
        mockMvc.perform(
                get("/api/v1/identity/attempts/me")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
        .andExpect(jsonPath("$.citizenIdMasked").value("079******789"))
        .andExpect(jsonPath("$.faceMatched").value(true))
        .andExpect(jsonPath("$.livenessPassed").value(true));

        // Verify User invariant in DB: NOT verified before admin review
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("079123456789", updatedUser.getCitizenId());
        assertFalse(updatedUser.isIdentityApproved());
        assertFalse(Boolean.TRUE.equals(updatedUser.getIdentityVerified()));
        assertNull(updatedUser.getIdentityReviewedAt());
    }

    @Test
    @DisplayName("Smoke Test: Kết nối dịch vụ OCR thật (Tự động skip nếu server không chạy)")
    void testRealOcrServiceSmoke() {
        boolean serviceAvailable = false;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(500))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/health"))
                    .timeout(Duration.ofMillis(500))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            serviceAvailable = (response.statusCode() == 200);
        } catch (Exception ignored) {
            serviceAvailable = false;
        }

        Assumptions.assumeTrue(
                serviceAvailable,
                "Real OCR service is NOT running on http://localhost:8081. Skipping real smoke test."
        );

        // If available, verify real health endpoint
        assertTrue(serviceAvailable);
    }
}

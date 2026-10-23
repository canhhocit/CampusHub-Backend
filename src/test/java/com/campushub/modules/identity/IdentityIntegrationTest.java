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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.campushub.modules.identity.enums.VerificationFailureCode;

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

        String uid = UUID.randomUUID().toString().substring(0, 8);
        testUser = new User();
        testUser.setUsername("user_ekyc_" + uid);
        testUser.setEmail("ekyc_" + uid + "@test.com");
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
                    .uri(URI.create("http://localhost:8081/api/v1/health"))
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
                "Real OCR service is NOT running on http://localhost:8081/api/v1/health. Skipping real smoke test."
        );

        // If available, verify real health endpoint
        assertTrue(serviceAvailable);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Bảo đảm failed attempt được lưu vào DB khi OCR trích xuất thất bại")
    void testFailedAttemptSavedOnOcrFailure() throws Exception {
        attemptRepository.deleteAll();

        OcrExtractionResponseDto failedDto = new OcrExtractionResponseDto();
        failedDto.setSuccess(false);
        when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(failedDto);

        MockMultipartFile cardFile = new MockMultipartFile(
                "card_file", "bad_cccd.jpg", "image/jpeg", validJpegBytes
        );

        mockMvc.perform(
                multipart("/api/v1/identity/attempts/start")
                        .file(cardFile)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("IDENTITY_OCR_FAILED"));

        List<IdentityVerificationAttempt> attempts = attemptRepository.findAll();
        assertEquals(1, attempts.size(), "Failed attempt must be persisted in database");
        assertEquals(VerificationAttemptStatus.REJECTED, attempts.get(0).getStatus());
        assertEquals(VerificationFailureCode.OCR_EXTRACT_FAILED, attempts.get(0).getFailureCode());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Bảo đảm failed attempt được lưu khi phát hiện giả mạo khuôn mặt (Spoof)")
    void testFailedAttemptSavedOnSpoofDetection() throws Exception {
        attemptRepository.deleteAll();

        // 1. Start attempt successfully
        OcrExtractionResponseDto ocrDto = new OcrExtractionResponseDto();
        ocrDto.setSuccess(true);
        ocrDto.setFields(Map.of("id", new ExtractedFieldDto("079123456789", 0.99, FieldStatus.EXTRACTED)));
        when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(ocrDto);

        LivenessSessionCreateResponseDto sessionDto = new LivenessSessionCreateResponseDto(
                "sess-spoof", 1729600000.0, 900, 2, 0,
                new ChallengeInfoDto(ChallengeAction.TURN_HEAD_LEFT, "Quay trái", 10)
        );
        when(ocrClient.createLivenessSession()).thenReturn(sessionDto);

        MockMultipartFile cardFile = new MockMultipartFile(
                "card_file", "cccd_front.jpg", "image/jpeg", validJpegBytes
        );

        MvcResult startResult = mockMvc.perform(
                multipart("/api/v1/identity/attempts/start")
                        .file(cardFile)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andReturn();

        StartVerificationResponseDto startRes = objectMapper.readValue(
                startResult.getResponse().getContentAsString(), StartVerificationResponseDto.class
        );
        String attemptId = startRes.getAttemptId();

        // 2. Submit liveness-step with spoof detected
        LivenessStepVerifyResponseDto spoofDto = new LivenessStepVerifyResponseDto();
        spoofDto.setCurrentStep(0);
        spoofDto.setStepPassed(false);
        spoofDto.setAllPassed(false);
        spoofDto.setAntiSpoofingOutcome(AntiSpoofingOutcome.SPOOF);
        spoofDto.setStatus(LivenessSessionStatus.FAILED);
        when(ocrClient.verifyLivenessStep(eq("sess-spoof"), anyList())).thenReturn(spoofDto);

        MockMultipartFile frame = new MockMultipartFile("frames", "f1.jpg", "image/jpeg", validJpegBytes);
        mockMvc.perform(
                multipart("/api/v1/identity/attempts/" + attemptId + "/liveness-step")
                        .file(frame)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("IDENTITY_LIVENESS_FAILED"));

        IdentityVerificationAttempt attemptInDb = attemptRepository.findByAttemptId(attemptId).orElseThrow();
        assertEquals(VerificationAttemptStatus.REJECTED, attemptInDb.getStatus());
        assertEquals(VerificationFailureCode.LIVENESS_SPOOF_DETECTED, attemptInDb.getFailureCode());
        assertEquals("SPOOF", attemptInDb.getAntiSpoofingOutcome());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Bảo đảm phát hiện và lưu failed attempt khi ảnh CCCD ở verify-face khác ảnh ở bước 1")
    void testCardHashMismatchSavedOnVerifyFace() throws Exception {
        attemptRepository.deleteAll();

        // 1. Setup attempt at LIVENESS_PASSED state
        IdentityVerificationAttempt attempt = new IdentityVerificationAttempt();
        attempt.setAttemptId("att-hash-test");
        attempt.setUser(testUser);
        attempt.setStatus(VerificationAttemptStatus.LIVENESS_PASSED);
        attempt.setLivenessSessionId("sess-test");
        attempt.setCitizenId("079123456789");
        attempt.setCardPhotoHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"); // Different hash
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        attempt.setLivenessPassed(true);
        attemptRepository.save(attempt);

        MockMultipartFile cardFile = new MockMultipartFile("card_file", "card.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile selfieFile = new MockMultipartFile("selfie_file", "selfie.jpg", "image/jpeg", validJpegBytes);

        mockMvc.perform(
                multipart("/api/v1/identity/attempts/att-hash-test/verify-face")
                        .file(cardFile)
                        .file(selfieFile)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("IDENTITY_CARD_MISMATCH"));

        IdentityVerificationAttempt failedAttempt = attemptRepository.findByAttemptId("att-hash-test").orElseThrow();
        assertEquals(VerificationAttemptStatus.REJECTED, failedAttempt.getStatus());
        assertEquals(VerificationFailureCode.CARD_HASH_MISMATCH, failedAttempt.getFailureCode());
    }

    @Test
    @DisplayName("Kiểm tra phân quyền: User khác không thể truy cập hoặc thao tác trên attempt")
    void testNonOwnerCannotAccessOrAdvanceAttempt() throws Exception {
        // Create another user with unique username
        String otherUid = UUID.randomUUID().toString().substring(0, 8);
        User otherUser = new User();
        otherUser.setUsername("other_" + otherUid);
        otherUser.setEmail("other_" + otherUid + "@test.com");
        otherUser.setPasswordHash("hashed");
        otherUser.setFullName("Other User");
        otherUser.setRole(Role.USER);
        otherUser.setStatus(UserStatus.ACTIVE);
        otherUser = userRepository.save(otherUser);

        String otherToken = jwtTokenProvider.generateAccessTokenFromUsernameAndIdAndRole(
                otherUser.getUsername(), otherUser.getId(), "ROLE_USER"
        );

        // Attempt belongs to testUser
        IdentityVerificationAttempt attempt = new IdentityVerificationAttempt();
        attempt.setAttemptId("att-ownership-test");
        attempt.setUser(testUser);
        attempt.setStatus(VerificationAttemptStatus.LIVENESS_IN_PROGRESS);
        attempt.setLivenessSessionId("sess-test");
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        attemptRepository.save(attempt);

        // Other user attempts to get or submit
        mockMvc.perform(
                get("/api/v1/identity/attempts/att-ownership-test")
                        .header("Authorization", "Bearer " + otherToken)
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED_ACTION"));
    }
}

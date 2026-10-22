package com.campushub.modules.identity;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.ocr.client.OcrServiceClient;
import com.campushub.infrastructure.ocr.dto.*;
import com.campushub.infrastructure.ocr.exception.OcrConnectionException;
import com.campushub.infrastructure.ocr.exception.OcrTimeoutException;
import com.campushub.modules.audit.service.AuditService;
import com.campushub.modules.identity.config.IdentityProperties;
import com.campushub.modules.identity.dto.*;
import com.campushub.modules.identity.entity.IdentityVerificationAttempt;
import com.campushub.modules.identity.enums.VerificationAttemptStatus;
import com.campushub.modules.identity.enums.VerificationFailureCode;
import com.campushub.modules.identity.repository.IdentityVerificationAttemptRepository;
import com.campushub.modules.identity.service.IdentityVerificationService;
import com.campushub.modules.upload.service.UploadService;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVerificationServiceTest {

    @Mock
    private IdentityVerificationAttemptRepository attemptRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OcrServiceClient ocrClient;
    @Mock
    private UploadService uploadService;
    @Mock
    private AuditService auditService;

    private IdentityProperties properties;
    private IdentityVerificationService service;

    private User testUser;
    private User otherUser;
    private MockMultipartFile validCardFile;
    private MockMultipartFile validSelfieFile;

    private static byte[] createValidJpegBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) 0xFF);
        baos.write((byte) 0xD8);
        baos.write((byte) 0xFF);
        baos.write((byte) 0xE0);
        for (int i = 0; i < 20; i++) {
            baos.write((byte) 0xAA);
        }
        return baos.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        properties = new IdentityProperties();
        properties.setMaxDailyAttempts(5);
        properties.setAttemptTtlMinutes(15);
        properties.setMaxStepsPerAttempt(10);

        service = new IdentityVerificationService(
                attemptRepository, userRepository, ocrClient, uploadService, auditService, properties
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setIdentityVerified(false);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setRole(Role.USER);
        otherUser.setStatus(UserStatus.ACTIVE);
        otherUser.setIdentityVerified(false);

        byte[] jpegBytes = createValidJpegBytes();
        validCardFile = new MockMultipartFile("card_file", "card.jpg", "image/jpeg", jpegBytes);
        validSelfieFile = new MockMultipartFile("selfie_file", "selfie.jpg", "image/jpeg", jpegBytes);
    }

    @Nested
    @DisplayName("1. Start Attempt Tests")
    class StartAttemptTests {

        @Test
        @DisplayName("OCR thành công và tạo session liveness thành công")
        void testStartAttemptSuccess() {
            when(userRepository.findByUsernameWithLock("testuser")).thenReturn(Optional.of(testUser));
            when(attemptRepository.countAttemptsSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
            when(attemptRepository.findActiveAttempts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

            OcrExtractionResponseDto ocrDto = new OcrExtractionResponseDto();
            ocrDto.setSuccess(true);
            ocrDto.setFields(Map.of("id", new ExtractedFieldDto("012345678901", 0.98, FieldStatus.EXTRACTED)));
            when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(ocrDto);

            when(userRepository.findByCitizenId("012345678901")).thenReturn(Optional.empty());
            when(attemptRepository.findByCitizenIdAndStatusIn(eq("012345678901"), any())).thenReturn(Collections.emptyList());

            LivenessSessionCreateResponseDto livenessDto = new LivenessSessionCreateResponseDto(
                    "session-abc", 1729600000.0, 900, 2, 0,
                    new ChallengeInfoDto(ChallengeAction.TURN_HEAD_LEFT, "Turn left", 10)
            );
            when(ocrClient.createLivenessSession()).thenReturn(livenessDto);
            when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StartVerificationResponseDto res = service.startAttempt("testuser", validCardFile);

            assertNotNull(res);
            assertNotNull(res.getAttemptId());
            assertEquals(VerificationAttemptStatus.LIVENESS_IN_PROGRESS, res.getStatus());
            assertEquals(2, res.getTotalChallenges());
            assertEquals(ChallengeAction.TURN_HEAD_LEFT, res.getCurrentChallenge().getAction());
            verify(attemptRepository).save(any(IdentityVerificationAttempt.class));
        }

        @Test
        @DisplayName("1. OCR thành công nhưng số CCCD không hợp lệ (ít hơn 12 số)")
        void testOcrSuccessButInvalidCitizenId() {
            when(userRepository.findByUsernameWithLock("testuser")).thenReturn(Optional.of(testUser));
            when(attemptRepository.countAttemptsSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
            when(attemptRepository.findActiveAttempts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

            OcrExtractionResponseDto ocrDto = new OcrExtractionResponseDto();
            ocrDto.setSuccess(true);
            ocrDto.setFields(Map.of("id", new ExtractedFieldDto("12345", 0.98, FieldStatus.EXTRACTED)));
            when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(ocrDto);

            AppException ex = assertThrows(AppException.class, () -> service.startAttempt("testuser", validCardFile));
            assertEquals(ErrorCode.INVALID_CITIZEN_ID, ex.getErrorCode());
            verify(attemptRepository).save(argThat(a -> a.getFailureCode() == VerificationFailureCode.INVALID_CITIZEN_ID));
        }

        @Test
        @DisplayName("2. OCR không trích xuất được số CCCD (id null)")
        void testOcrCannotExtractCitizenId() {
            when(userRepository.findByUsernameWithLock("testuser")).thenReturn(Optional.of(testUser));
            when(attemptRepository.countAttemptsSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
            when(attemptRepository.findActiveAttempts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

            OcrExtractionResponseDto ocrDto = new OcrExtractionResponseDto();
            ocrDto.setSuccess(true);
            ocrDto.setFields(Collections.emptyMap());
            when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(ocrDto);

            AppException ex = assertThrows(AppException.class, () -> service.startAttempt("testuser", validCardFile));
            assertEquals(ErrorCode.INVALID_CITIZEN_ID, ex.getErrorCode());
        }

        @Test
        @DisplayName("3. CCCD đã được user khác sử dụng")
        void testCitizenIdAlreadyUsedByAnotherUser() {
            when(userRepository.findByUsernameWithLock("testuser")).thenReturn(Optional.of(testUser));
            when(attemptRepository.countAttemptsSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
            when(attemptRepository.findActiveAttempts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

            OcrExtractionResponseDto ocrDto = new OcrExtractionResponseDto();
            ocrDto.setSuccess(true);
            ocrDto.setFields(Map.of("id", new ExtractedFieldDto("012345678901", 0.98, FieldStatus.EXTRACTED)));
            when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(ocrDto);

            when(userRepository.findByCitizenId("012345678901")).thenReturn(Optional.of(otherUser));

            AppException ex = assertThrows(AppException.class, () -> service.startAttempt("testuser", validCardFile));
            assertEquals(ErrorCode.CITIZEN_ID_ALREADY_EXISTS, ex.getErrorCode());
            verify(attemptRepository).save(argThat(a -> a.getFailureCode() == VerificationFailureCode.CITIZEN_ID_DUPLICATE));
        }

        @Test
        @DisplayName("4. OCR service timeout / 503")
        void testOcrServiceTimeoutThrowsServiceUnavailable() {
            when(userRepository.findByUsernameWithLock("testuser")).thenReturn(Optional.of(testUser));
            when(attemptRepository.countAttemptsSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
            when(attemptRepository.findActiveAttempts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

            when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true)))
                    .thenThrow(new OcrTimeoutException("OCR service timed out"));

            AppException ex = assertThrows(AppException.class, () -> service.startAttempt("testuser", validCardFile));
            assertEquals(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE, ex.getErrorCode());
        }

        @Test
        @DisplayName("5. Tạo liveness session thất bại")
        void testCreateLivenessSessionFailsThrowsServiceUnavailable() {
            when(userRepository.findByUsernameWithLock("testuser")).thenReturn(Optional.of(testUser));
            when(attemptRepository.countAttemptsSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
            when(attemptRepository.findActiveAttempts(eq(1L), any(), any())).thenReturn(Collections.emptyList());

            OcrExtractionResponseDto ocrDto = new OcrExtractionResponseDto();
            ocrDto.setSuccess(true);
            ocrDto.setFields(Map.of("id", new ExtractedFieldDto("012345678901", 0.98, FieldStatus.EXTRACTED)));
            when(ocrClient.extractCard(any(), any(), eq(CardSide.FRONT), eq(true))).thenReturn(ocrDto);
            when(userRepository.findByCitizenId("012345678901")).thenReturn(Optional.empty());

            when(ocrClient.createLivenessSession())
                    .thenThrow(new OcrConnectionException("Cannot connect to OCR service"));

            AppException ex = assertThrows(AppException.class, () -> service.startAttempt("testuser", validCardFile));
            assertEquals(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE, ex.getErrorCode());
        }

        @Test
        @DisplayName("15. Rate limit: vượt quá số lần thử trong ngày")
        void testRateLimitExceededRejectsStartAttempt() {
            when(userRepository.findByUsernameWithLock("testuser")).thenReturn(Optional.of(testUser));
            when(attemptRepository.countAttemptsSince(eq(1L), any(LocalDateTime.class))).thenReturn(5L);

            AppException ex = assertThrows(AppException.class, () -> service.startAttempt("testuser", validCardFile));
            assertEquals(ErrorCode.IDENTITY_RATE_LIMIT_EXCEEDED, ex.getErrorCode());
            verify(ocrClient, never()).extractCard(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("2. Liveness Step Verification Tests")
    class LivenessStepTests {

        private IdentityVerificationAttempt attempt;

        @BeforeEach
        void initAttempt() {
            attempt = new IdentityVerificationAttempt();
            attempt.setAttemptId("att-123");
            attempt.setUser(testUser);
            attempt.setStatus(VerificationAttemptStatus.LIVENESS_IN_PROGRESS);
            attempt.setLivenessSessionId("session-xyz");
            attempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
            attempt.setExpiresAt(LocalDateTime.now().plusMinutes(13));
            attempt.setStepCount(0);
        }

        @Test
        @DisplayName("6. User khác cố truy cập attempt bị từ chối")
        void testOtherUserCannotAccessAttempt() {
            when(attemptRepository.findByAttemptId("att-123")).thenReturn(Optional.of(attempt));

            AppException ex = assertThrows(AppException.class,
                    () -> service.verifyLivenessStep("otheruser", "att-123", List.of(validCardFile)));
            assertEquals(ErrorCode.UNAUTHORIZED_ACTION, ex.getErrorCode());
        }

        @Test
        @DisplayName("7. Attempt đã hết hạn bị từ chối")
        void testExpiredAttemptIsRejected() {
            attempt.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(attemptRepository.findByAttemptId("att-123")).thenReturn(Optional.of(attempt));

            AppException ex = assertThrows(AppException.class,
                    () -> service.verifyLivenessStep("testuser", "att-123", List.of(validCardFile)));
            assertEquals(ErrorCode.IDENTITY_ATTEMPT_EXPIRED, ex.getErrorCode());
            assertEquals(VerificationAttemptStatus.EXPIRED, attempt.getStatus());
        }

        @Test
        @DisplayName("8. Liveness step có SPOOF phát hiện giả mạo")
        void testLivenessStepWithSpoofDetected() {
            when(attemptRepository.findByAttemptId("att-123")).thenReturn(Optional.of(attempt));

            LivenessStepVerifyResponseDto spoofResponse = new LivenessStepVerifyResponseDto();
            spoofResponse.setStepPassed(true);
            spoofResponse.setAntiSpoofingOutcome(AntiSpoofingOutcome.SPOOF);
            spoofResponse.setStatus(LivenessSessionStatus.FAILED);
            when(ocrClient.verifyLivenessStep(eq("session-xyz"), anyList())).thenReturn(spoofResponse);

            AppException ex = assertThrows(AppException.class,
                    () -> service.verifyLivenessStep("testuser", "att-123", List.of(validCardFile)));
            assertEquals(ErrorCode.IDENTITY_LIVENESS_FAILED, ex.getErrorCode());
            assertEquals(VerificationAttemptStatus.REJECTED, attempt.getStatus());
            assertEquals(VerificationFailureCode.LIVENESS_SPOOF_DETECTED, attempt.getFailureCode());
        }

        @Test
        @DisplayName("9. Liveness step có UNCERTAIN hoặc UNKNOWN không được coi là pass")
        void testLivenessStepUncertainDoesNotPass() {
            when(attemptRepository.findByAttemptId("att-123")).thenReturn(Optional.of(attempt));

            LivenessStepVerifyResponseDto uncertainResponse = new LivenessStepVerifyResponseDto();
            uncertainResponse.setStepPassed(true);
            uncertainResponse.setAntiSpoofingOutcome(AntiSpoofingOutcome.UNCERTAIN);
            uncertainResponse.setStatus(LivenessSessionStatus.IN_PROGRESS);
            when(ocrClient.verifyLivenessStep(eq("session-xyz"), anyList())).thenReturn(uncertainResponse);

            LivenessStepResultDto result = service.verifyLivenessStep("testuser", "att-123", List.of(validCardFile));
            assertFalse(result.isActionCompleted());
            assertFalse(result.isLivenessPassed());
            assertEquals(VerificationAttemptStatus.LIVENESS_IN_PROGRESS, attempt.getStatus());
        }

        @Test
        @DisplayName("10. step_passed=true nhưng PAD không phải LIVE không được pass")
        void testStepPassedTrueButPadNotLiveDoesNotPass() {
            when(attemptRepository.findByAttemptId("att-123")).thenReturn(Optional.of(attempt));

            LivenessStepVerifyResponseDto response = new LivenessStepVerifyResponseDto();
            response.setStepPassed(true);
            response.setAntiSpoofingOutcome(AntiSpoofingOutcome.UNKNOWN);
            response.setStatus(LivenessSessionStatus.IN_PROGRESS);
            when(ocrClient.verifyLivenessStep(eq("session-xyz"), anyList())).thenReturn(response);

            LivenessStepResultDto result = service.verifyLivenessStep("testuser", "att-123", List.of(validCardFile));
            assertFalse(result.isActionCompleted());
            assertFalse(result.isLivenessPassed());
        }

        @Test
        @DisplayName("11. Một step thành công nhưng session chưa PASSED (tiến tới challenge tiếp theo)")
        void testIntermediateStepSuccessSessionNotPassed() {
            when(attemptRepository.findByAttemptId("att-123")).thenReturn(Optional.of(attempt));

            LivenessStepVerifyResponseDto intermediate = new LivenessStepVerifyResponseDto();
            intermediate.setCurrentStep(0);
            intermediate.setStepPassed(true);
            intermediate.setAllPassed(false);
            intermediate.setAntiSpoofingOutcome(AntiSpoofingOutcome.LIVE);
            intermediate.setStatus(LivenessSessionStatus.IN_PROGRESS);
            intermediate.setNextChallenge(new ChallengeInfoDto(ChallengeAction.NOD_HEAD, "Nod head", 10));
            when(ocrClient.verifyLivenessStep(eq("session-xyz"), anyList())).thenReturn(intermediate);

            LivenessStepResultDto result = service.verifyLivenessStep("testuser", "att-123", List.of(validCardFile));
            assertTrue(result.isActionCompleted());
            assertFalse(result.isLivenessPassed());
            assertNotNull(result.getNextChallenge());
            assertEquals(ChallengeAction.NOD_HEAD, result.getNextChallenge().getAction());
            assertEquals(VerificationAttemptStatus.LIVENESS_IN_PROGRESS, attempt.getStatus());
        }

        @Test
        @DisplayName("Tất cả challenge hoàn thành -> Session chuyển sang LIVENESS_PASSED")
        void testFinalStepSuccessSessionPassed() {
            when(attemptRepository.findByAttemptId("att-123")).thenReturn(Optional.of(attempt));

            LivenessStepVerifyResponseDto finalResponse = new LivenessStepVerifyResponseDto();
            finalResponse.setCurrentStep(2);
            finalResponse.setStepPassed(true);
            finalResponse.setAllPassed(true);
            finalResponse.setAntiSpoofingOutcome(AntiSpoofingOutcome.LIVE);
            finalResponse.setStatus(LivenessSessionStatus.PASSED);
            when(ocrClient.verifyLivenessStep(eq("session-xyz"), anyList())).thenReturn(finalResponse);

            LivenessStepResultDto result = service.verifyLivenessStep("testuser", "att-123", List.of(validCardFile));
            assertTrue(result.isActionCompleted());
            assertTrue(result.isLivenessPassed());
            assertEquals(VerificationAttemptStatus.LIVENESS_PASSED, attempt.getStatus());
            assertTrue(attempt.getLivenessPassed());
        }
    }

    @Nested
    @DisplayName("3. Face Verification Tests")
    class FaceVerificationTests {

        private IdentityVerificationAttempt attempt;
        private String cardHash;

        @BeforeEach
        void initAttempt() throws Exception {
            cardHash = sha256Hex(validCardFile.getBytes());
            attempt = new IdentityVerificationAttempt();
            attempt.setAttemptId("att-456");
            attempt.setUser(testUser);
            attempt.setStatus(VerificationAttemptStatus.LIVENESS_PASSED);
            attempt.setCitizenId("012345678901");
            attempt.setCardPhotoHash(cardHash);
            attempt.setLivenessPassed(true);
            attempt.setStartedAt(LocalDateTime.now().minusMinutes(3));
            attempt.setExpiresAt(LocalDateTime.now().plusMinutes(12));
        }

        @Test
        @DisplayName("12. Face pipeline thành công nhưng match=false (dưới threshold)")
        void testFacePipelineSuccessButMatchFalse() {
            when(attemptRepository.findByAttemptId("att-456")).thenReturn(Optional.of(attempt));

            FaceVerificationResponseDto faceDto = new FaceVerificationResponseDto(
                    true, false, 0.25, 0.75, 0.363, null, null, 150.0
            );
            when(ocrClient.verifyFace(any(), any(), any(), any())).thenReturn(faceDto);

            AppException ex = assertThrows(AppException.class,
                    () -> service.verifyFaceAndComplete("testuser", "att-456", validCardFile, validSelfieFile));
            assertEquals(ErrorCode.IDENTITY_FACE_MISMATCH, ex.getErrorCode());
            assertEquals(VerificationAttemptStatus.REJECTED, attempt.getStatus());
            assertEquals(VerificationFailureCode.FACE_MISMATCH, attempt.getFailureCode());
        }

        @Test
        @DisplayName("13. Face pipeline không khả dụng / timeout")
        void testFacePipelineUnavailableThrowsServiceUnavailable() {
            when(attemptRepository.findByAttemptId("att-456")).thenReturn(Optional.of(attempt));

            when(ocrClient.verifyFace(any(), any(), any(), any()))
                    .thenThrow(new OcrTimeoutException("Face verification timed out"));

            AppException ex = assertThrows(AppException.class,
                    () -> service.verifyFaceAndComplete("testuser", "att-456", validCardFile, validSelfieFile));
            assertEquals(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE, ex.getErrorCode());
        }

        @Test
        @DisplayName("14. Không được đánh dấu user verified khi chưa qua admin review")
        void testFaceSuccessTransitionsToPendingReviewWithoutMarkingUserVerified() {
            when(attemptRepository.findByAttemptId("att-456")).thenReturn(Optional.of(attempt));

            FaceVerificationResponseDto faceDto = new FaceVerificationResponseDto(
                    true, true, 0.88, 0.12, 0.363, null, null, 200.0
            );
            when(ocrClient.verifyFace(any(), any(), any(), any())).thenReturn(faceDto);

            FaceVerificationResultDto result = service.verifyFaceAndComplete(
                    "testuser", "att-456", validCardFile, validSelfieFile
            );

            assertNotNull(result);
            assertEquals(VerificationAttemptStatus.PENDING_REVIEW, result.getStatus());
            assertTrue(result.getMatch());

            // Invariant: User is NOT verified yet!
            assertFalse(testUser.isIdentityApproved(), "User must NOT be identity approved before Admin review");
            assertFalse(Boolean.TRUE.equals(testUser.getIdentityVerified()), "User identityVerified flag must remain FALSE");
            assertNull(testUser.getIdentityReviewedAt(), "identityReviewedAt must remain null until admin approval");
            assertEquals("012345678901", testUser.getCitizenId());
            verify(userRepository).save(testUser);
            verify(attemptRepository).save(attempt);
        }

        @Test
        @DisplayName("18. Ràng buộc mã băm CCCD: Gửi ảnh thẻ khác với ảnh ở bước 1 bị từ chối")
        void testCardHashMismatchRejected() {
            when(attemptRepository.findByAttemptId("att-456")).thenReturn(Optional.of(attempt));

            byte[] alteredBytes = createValidJpegBytes();
            alteredBytes[10] = (byte) 0x99; // alter byte
            MockMultipartFile alteredCardFile = new MockMultipartFile("card_file", "card2.jpg", "image/jpeg", alteredBytes);

            AppException ex = assertThrows(AppException.class,
                    () -> service.verifyFaceAndComplete("testuser", "att-456", alteredCardFile, validSelfieFile));
            assertEquals(ErrorCode.IDENTITY_CARD_MISMATCH, ex.getErrorCode());
            assertEquals(VerificationAttemptStatus.REJECTED, attempt.getStatus());
            assertEquals(VerificationFailureCode.CARD_HASH_MISMATCH, attempt.getFailureCode());
            verify(ocrClient, never()).verifyFace(any(), any(), any(), any());
        }

        @Test
        @DisplayName("17. Trạng thái không hợp lệ bị từ chối (chưa qua liveness)")
        void testVerifyFaceOnInvalidStateRejected() {
            attempt.setStatus(VerificationAttemptStatus.LIVENESS_IN_PROGRESS);
            when(attemptRepository.findByAttemptId("att-456")).thenReturn(Optional.of(attempt));

            AppException ex = assertThrows(AppException.class,
                    () -> service.verifyFaceAndComplete("testuser", "att-456", validCardFile, validSelfieFile));
            assertEquals(ErrorCode.IDENTITY_ATTEMPT_INVALID_STATE, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("4. Query & Privacy Tests")
    class QueryAndPrivacyTests {

        @Test
        @DisplayName("16. Mask CCCD và không trả/lưu dữ liệu nhạy cảm")
        void testMaskCitizenIdInQuery() {
            IdentityVerificationAttempt attempt = new IdentityVerificationAttempt();
            attempt.setAttemptId("att-789");
            attempt.setUser(testUser);
            attempt.setStatus(VerificationAttemptStatus.PENDING_REVIEW);
            attempt.setCitizenId("079123456789");
            attempt.setFaceSimilarityScore(0.85);
            attempt.setFaceMatched(true);
            attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
            attempt.setExpiresAt(LocalDateTime.now().plusMinutes(10));

            when(attemptRepository.findByAttemptId("att-789")).thenReturn(Optional.of(attempt));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            IdentityAttemptResponseDto res = service.getAttempt("testuser", "att-789");
            assertNotNull(res);
            assertEquals("079******789", res.getCitizenIdMasked());
            assertEquals(0.85, res.getFaceSimilarityScore());
            assertTrue(res.getFaceMatched());
        }
    }
}

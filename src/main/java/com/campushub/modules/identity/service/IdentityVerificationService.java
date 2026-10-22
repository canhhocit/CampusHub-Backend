package com.campushub.modules.identity.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.ocr.client.OcrServiceClient;
import com.campushub.infrastructure.ocr.dto.*;
import com.campushub.infrastructure.ocr.exception.OcrConnectionException;
import com.campushub.infrastructure.ocr.exception.OcrTimeoutException;
import com.campushub.modules.audit.entity.AuditAction;
import com.campushub.modules.audit.entity.AuditEntityType;
import com.campushub.modules.audit.service.AuditService;
import com.campushub.modules.identity.config.IdentityProperties;
import com.campushub.modules.identity.dto.*;
import com.campushub.modules.identity.entity.IdentityVerificationAttempt;
import com.campushub.modules.identity.enums.VerificationAttemptStatus;
import com.campushub.modules.identity.enums.VerificationFailureCode;
import com.campushub.modules.identity.repository.IdentityVerificationAttemptRepository;
import com.campushub.modules.upload.service.UploadService;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class IdentityVerificationService {

    private static final Logger log = LoggerFactory.getLogger(IdentityVerificationService.class);
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final IdentityVerificationAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final OcrServiceClient ocrClient;
    private final UploadService uploadService;
    private final AuditService auditService;
    private final IdentityProperties properties;

    public IdentityVerificationService(
            IdentityVerificationAttemptRepository attemptRepository,
            UserRepository userRepository,
            OcrServiceClient ocrClient,
            UploadService uploadService,
            AuditService auditService,
            IdentityProperties properties) {
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.ocrClient = ocrClient;
        this.uploadService = uploadService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public StartVerificationResponseDto startAttempt(String username, MultipartFile cardFile) {
        User user = userRepository.findByUsernameWithLock(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isIdentityApproved()) {
            throw new AppException(ErrorCode.ALREADY_IDENTITY_VERIFIED);
        }

        // 1. Rate limiting by user per day
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long attemptsToday = attemptRepository.countAttemptsSince(user.getId(), startOfDay);
        if (attemptsToday >= properties.getMaxDailyAttempts()) {
            throw new AppException(ErrorCode.IDENTITY_RATE_LIMIT_EXCEEDED);
        }

        // 2. Expire any stale active in-progress attempts for this user
        List<IdentityVerificationAttempt> activeAttempts = attemptRepository.findActiveAttempts(
                user.getId(),
                List.of(VerificationAttemptStatus.STARTED, VerificationAttemptStatus.LIVENESS_IN_PROGRESS),
                LocalDateTime.now()
        );
        for (IdentityVerificationAttempt stale : activeAttempts) {
            stale.markExpired();
            attemptRepository.save(stale);
        }

        // 3. Validate image
        byte[] cardBytes = validateAndExtractBytes(cardFile);
        String cardPhotoHash = sha256Hex(cardBytes);

        // 4. OCR Extraction
        OcrExtractionResponseDto ocrResult;
        try {
            ocrResult = ocrClient.extractCard(cardBytes, cardFile.getOriginalFilename(), CardSide.FRONT, true);
        } catch (OcrConnectionException | OcrTimeoutException e) {
            log.error("OCR Service unavailable during startAttempt: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("OCR Service call failed: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_OCR_FAILED);
        }

        if (ocrResult == null || !ocrResult.isSuccess()) {
            IdentityVerificationAttempt failedAttempt = createFailedAttempt(
                    user, cardPhotoHash, VerificationFailureCode.OCR_EXTRACT_FAILED,
                    "Không thể trích xuất thông tin hợp lệ từ thẻ CCCD"
            );
            attemptRepository.save(failedAttempt);
            throw new AppException(ErrorCode.IDENTITY_OCR_FAILED);
        }

        String citizenId = ocrResult.getCitizenId();
        if (citizenId == null || !citizenId.trim().matches("^[0-9]{12}$")) {
            IdentityVerificationAttempt failedAttempt = createFailedAttempt(
                    user, cardPhotoHash, VerificationFailureCode.INVALID_CITIZEN_ID,
                    "Số CCCD không đúng định dạng 12 chữ số"
            );
            attemptRepository.save(failedAttempt);
            throw new AppException(ErrorCode.INVALID_CITIZEN_ID);
        }
        citizenId = citizenId.trim();

        // 5. Duplicate citizenId checks
        Optional<User> existingUser = userRepository.findByCitizenId(citizenId);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            IdentityVerificationAttempt failedAttempt = createFailedAttempt(
                    user, cardPhotoHash, VerificationFailureCode.CITIZEN_ID_DUPLICATE,
                    "Số CCCD đã được sử dụng bởi tài khoản khác"
            );
            failedAttempt.setCitizenId(citizenId);
            attemptRepository.save(failedAttempt);
            throw new AppException(ErrorCode.CITIZEN_ID_ALREADY_EXISTS);
        }

        List<IdentityVerificationAttempt> conflictingAttempts = attemptRepository.findByCitizenIdAndStatusIn(
                citizenId,
                List.of(VerificationAttemptStatus.PENDING_REVIEW, VerificationAttemptStatus.FACE_VERIFICATION_PASSED)
        );
        for (IdentityVerificationAttempt conflicting : conflictingAttempts) {
            if (!conflicting.getUser().getId().equals(user.getId())) {
                IdentityVerificationAttempt failedAttempt = createFailedAttempt(
                        user, cardPhotoHash, VerificationFailureCode.CITIZEN_ID_DUPLICATE,
                        "Số CCCD đang chờ duyệt bởi tài khoản khác"
                );
                failedAttempt.setCitizenId(citizenId);
                attemptRepository.save(failedAttempt);
                throw new AppException(ErrorCode.CITIZEN_ID_ALREADY_EXISTS);
            }
        }

        // 6. Create Liveness Session with OCR service
        LivenessSessionCreateResponseDto livenessSession;
        try {
            livenessSession = ocrClient.createLivenessSession();
        } catch (OcrConnectionException | OcrTimeoutException e) {
            log.error("OCR Service unavailable during createLivenessSession: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Failed to create liveness session: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE);
        }

        if (livenessSession == null || livenessSession.getSessionId() == null) {
            throw new AppException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE);
        }

        // 7. Persist new Attempt
        String attemptId = UUID.randomUUID().toString();
        IdentityVerificationAttempt attempt = new IdentityVerificationAttempt();
        attempt.setAttemptId(attemptId);
        attempt.setUser(user);
        attempt.setStatus(VerificationAttemptStatus.LIVENESS_IN_PROGRESS);
        attempt.setLivenessSessionId(livenessSession.getSessionId());
        attempt.setCitizenId(citizenId);
        attempt.setCardPhotoHash(cardPhotoHash);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getAttemptTtlMinutes()));
        attempt.setStepCount(0);

        attemptRepository.save(attempt);

        auditService.log(AuditAction.IDENTITY_ATTEMPT_START, AuditEntityType.USER, user.getId(),
                "Started eKYC attempt " + attemptId);

        ChallengeInfoDto currentChallenge = livenessSession.getChallenge();
        int totalChallenges = livenessSession.getTotalSteps() != null ? livenessSession.getTotalSteps() : 3;
        int currentStep = livenessSession.getCurrentStep() != null ? livenessSession.getCurrentStep() : 0;
        return new StartVerificationResponseDto(
                attemptId,
                attempt.getStatus(),
                attempt.getExpiresAt(),
                currentChallenge,
                totalChallenges,
                currentStep
        );
    }

    @Transactional
    public LivenessStepResultDto verifyLivenessStep(String username, String attemptId, List<MultipartFile> frames) {
        IdentityVerificationAttempt attempt = attemptRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.IDENTITY_ATTEMPT_NOT_FOUND));

        if (!attempt.getUser().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (attempt.isExpired()) {
            attempt.markExpired();
            attemptRepository.save(attempt);
            throw new AppException(ErrorCode.IDENTITY_ATTEMPT_EXPIRED);
        }

        if (!attempt.getStatus().canPerformLiveness()) {
            throw new AppException(ErrorCode.IDENTITY_ATTEMPT_INVALID_STATE);
        }

        if (attempt.getStepCount() >= properties.getMaxStepsPerAttempt()) {
            attempt.markFailed(VerificationFailureCode.RATE_LIMIT_EXCEEDED, "Vượt quá số lần thử cử động liveness tối đa");
            attemptRepository.save(attempt);
            throw new AppException(ErrorCode.IDENTITY_RATE_LIMIT_EXCEEDED);
        }

        if (frames == null || frames.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        if (frames.size() > 30) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        List<byte[]> frameBytesList = new ArrayList<>(frames.size());
        for (MultipartFile frame : frames) {
            frameBytesList.add(validateAndExtractBytes(frame));
        }

        attempt.setStepCount(attempt.getStepCount() + 1);

        LivenessStepVerifyResponseDto stepResponse;
        try {
            stepResponse = ocrClient.verifyLivenessStep(attempt.getLivenessSessionId(), frameBytesList);
        } catch (OcrConnectionException | OcrTimeoutException e) {
            log.error("OCR Service timeout/unavailable during liveness step: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Liveness step call failed: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_LIVENESS_FAILED);
        }

        if (stepResponse == null) {
            throw new AppException(ErrorCode.IDENTITY_LIVENESS_FAILED);
        }

        // Anti-spoofing check
        if (stepResponse.isSpoofDetected()) {
            attempt.markFailed(VerificationFailureCode.LIVENESS_SPOOF_DETECTED, "Phát hiện giả mạo khuôn mặt (Spoof)");
            attempt.setAntiSpoofingOutcome(stepResponse.getAntiSpoofingOutcome() != null ? stepResponse.getAntiSpoofingOutcome().name() : "SPOOF");
            attemptRepository.save(attempt);

            auditService.log(AuditAction.IDENTITY_ATTEMPT_FAILED, AuditEntityType.USER, attempt.getUser().getId(),
                    "Spoof detected in attempt " + attemptId);
            throw new AppException(ErrorCode.IDENTITY_LIVENESS_FAILED);
        }

        int currentStep = stepResponse.getCurrentStep() != null ? stepResponse.getCurrentStep() : 0;
        boolean stepPassed = stepResponse.isStepSuccessful();
        if (!stepPassed) {
            attemptRepository.save(attempt);
            return new LivenessStepResultDto(
                    attemptId,
                    attempt.getStatus(),
                    currentStep,
                    false,
                    false,
                    stepResponse.getNextChallenge(),
                    stepResponse.getMessage() != null ? stepResponse.getMessage() : "Cử động chưa đạt. Vui lòng thực hiện lại."
            );
        }

        // Step action passed and PAD is LIVE
        if (stepResponse.isSessionFullyPassed()) {
            attempt.setStatus(VerificationAttemptStatus.LIVENESS_PASSED);
            attempt.setLivenessPassed(true);
            attempt.setAntiSpoofingOutcome("LIVE");
            attemptRepository.save(attempt);

            auditService.log(AuditAction.IDENTITY_LIVENESS_STEP, AuditEntityType.USER, attempt.getUser().getId(),
                    "Liveness fully passed for attempt " + attemptId);

            return new LivenessStepResultDto(
                    attemptId,
                    attempt.getStatus(),
                    currentStep,
                    true,
                    true,
                    null,
                    "Xác minh cử động hoàn tất. Vui lòng tiếp tục bước đối chiếu khuôn mặt."
            );
        } else {
            attemptRepository.save(attempt);
            auditService.log(AuditAction.IDENTITY_LIVENESS_STEP, AuditEntityType.USER, attempt.getUser().getId(),
                    "Liveness step " + currentStep + " completed for attempt " + attemptId);

            return new LivenessStepResultDto(
                    attemptId,
                    attempt.getStatus(),
                    currentStep,
                    true,
                    false,
                    stepResponse.getNextChallenge(),
                    "Bước " + (currentStep + 1) + " thành công. Vui lòng thực hiện cử động tiếp theo."
            );
        }
    }

    @Transactional
    public FaceVerificationResultDto verifyFaceAndComplete(
            String username, String attemptId, MultipartFile cardFile, MultipartFile selfieFile) {

        IdentityVerificationAttempt attempt = attemptRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.IDENTITY_ATTEMPT_NOT_FOUND));

        if (!attempt.getUser().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (attempt.isExpired()) {
            attempt.markExpired();
            attemptRepository.save(attempt);
            throw new AppException(ErrorCode.IDENTITY_ATTEMPT_EXPIRED);
        }

        if (!attempt.getStatus().canVerifyFace()) {
            throw new AppException(ErrorCode.IDENTITY_ATTEMPT_INVALID_STATE);
        }

        byte[] cardBytes = validateAndExtractBytes(cardFile);
        byte[] selfieBytes = validateAndExtractBytes(selfieFile);

        // Cryptographic card binding check
        String cardHash = sha256Hex(cardBytes);
        if (!cardHash.equalsIgnoreCase(attempt.getCardPhotoHash())) {
            attempt.markFailed(VerificationFailureCode.CARD_HASH_MISMATCH, "Ảnh CCCD không khớp với ảnh đã OCR ở bước 1");
            attemptRepository.save(attempt);

            auditService.log(AuditAction.IDENTITY_ATTEMPT_FAILED, AuditEntityType.USER, attempt.getUser().getId(),
                    "Card photo hash mismatch for attempt " + attemptId);
            throw new AppException(ErrorCode.IDENTITY_CARD_MISMATCH);
        }

        FaceVerificationResponseDto faceResponse;
        try {
            faceResponse = ocrClient.verifyFace(
                    cardBytes, cardFile.getOriginalFilename(),
                    selfieBytes, selfieFile.getOriginalFilename()
            );
        } catch (OcrConnectionException | OcrTimeoutException e) {
            log.error("OCR Service timeout/unavailable during verifyFace: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Face verification call failed: {}", e.getMessage());
            throw new AppException(ErrorCode.IDENTITY_FACE_MISMATCH);
        }

        if (faceResponse == null || !faceResponse.isSuccess()) {
            attempt.markFailed(VerificationFailureCode.FACE_MISMATCH, "Dịch vụ đối chiếu khuôn mặt không trích xuất được khuôn mặt");
            attemptRepository.save(attempt);
            throw new AppException(ErrorCode.IDENTITY_FACE_MISMATCH);
        }

        attempt.setFaceSimilarityScore(faceResponse.getSimilarityScore());
        attempt.setFaceThreshold(faceResponse.getThreshold());
        attempt.setFaceMatched(faceResponse.getMatch());

        if (!Boolean.TRUE.equals(faceResponse.getMatch())) {
            attempt.markFailed(VerificationFailureCode.FACE_MISMATCH,
                    "Độ tương đồng (" + faceResponse.getSimilarityScore() + ") không đạt ngưỡng " + faceResponse.getThreshold());
            attemptRepository.save(attempt);

            auditService.log(AuditAction.IDENTITY_ATTEMPT_FAILED, AuditEntityType.USER, attempt.getUser().getId(),
                    "Face match failed for attempt " + attemptId);
            throw new AppException(ErrorCode.IDENTITY_FACE_MISMATCH);
        }

        // Automated pipeline succeeded -> transition to PENDING_REVIEW
        attempt.setStatus(VerificationAttemptStatus.PENDING_REVIEW);
        attempt.setCompletedAt(LocalDateTime.now());

        // Upload card and selfie for administrative review
        try {
            String frontUrl = uploadService.uploadImage(cardFile);
            attempt.setIdCardFrontUrl(frontUrl);
        } catch (Exception e) {
            log.warn("Could not upload card front to media store: {}", e.getMessage());
        }

        try {
            String selfieUrl = uploadService.uploadImage(selfieFile);
            attempt.setSelfieUrl(selfieUrl);
        } catch (Exception e) {
            log.warn("Could not upload selfie to media store: {}", e.getMessage());
        }

        // Update user state for review (NOT automatically verified)
        User user = attempt.getUser();
        user.setCitizenId(attempt.getCitizenId());
        if (attempt.getIdCardFrontUrl() != null) {
            user.setIdCardFrontUrl(attempt.getIdCardFrontUrl());
        }
        user.setIdentityVerified(false);
        user.setIdentityVerifiedAt(null);
        user.setIdentityReviewedAt(null); // Pending human review

        userRepository.save(user);
        attemptRepository.save(attempt);

        auditService.log(AuditAction.IDENTITY_ATTEMPT_COMPLETE, AuditEntityType.USER, user.getId(),
                "Completed eKYC attempt " + attemptId + ". Status set to PENDING_REVIEW.");

        return new FaceVerificationResultDto(
                attemptId,
                attempt.getStatus(),
                faceResponse.getSimilarityScore(),
                faceResponse.getThreshold(),
                faceResponse.getMatch(),
                "Xác minh khuôn mặt thành công. Hồ sơ đã được chuyển sang trạng thái chờ Quản trị viên phê duyệt."
        );
    }

    @Transactional(readOnly = true)
    public IdentityAttemptResponseDto getAttempt(String username, String attemptId) {
        IdentityVerificationAttempt attempt = attemptRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.IDENTITY_ATTEMPT_NOT_FOUND));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isAdmin = user.getRole() == com.campushub.common.enums.Role.ADMIN;
        if (!attempt.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        return mapToResponseDto(attempt);
    }

    @Transactional(readOnly = true)
    public Optional<IdentityAttemptResponseDto> getLatestAttempt(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return attemptRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(this::mapToResponseDto);
    }

    private IdentityAttemptResponseDto mapToResponseDto(IdentityVerificationAttempt attempt) {
        IdentityAttemptResponseDto dto = new IdentityAttemptResponseDto();
        dto.setAttemptId(attempt.getAttemptId());
        dto.setStatus(attempt.getStatus());
        dto.setCitizenIdMasked(IdentityAttemptResponseDto.maskCitizenId(attempt.getCitizenId()));
        dto.setStartedAt(attempt.getStartedAt());
        dto.setExpiresAt(attempt.getExpiresAt());
        dto.setCompletedAt(attempt.getCompletedAt());
        dto.setFaceSimilarityScore(attempt.getFaceSimilarityScore());
        dto.setFaceThreshold(attempt.getFaceThreshold());
        dto.setFaceMatched(attempt.getFaceMatched());
        dto.setLivenessPassed(attempt.getLivenessPassed());
        dto.setAntiSpoofingOutcome(attempt.getAntiSpoofingOutcome());
        dto.setFailureCode(attempt.getFailureCode());
        dto.setFailureReason(attempt.getFailureReason());
        return dto;
    }

    private IdentityVerificationAttempt createFailedAttempt(User user, String cardHash, VerificationFailureCode code, String reason) {
        IdentityVerificationAttempt attempt = new IdentityVerificationAttempt();
        attempt.setAttemptId(UUID.randomUUID().toString());
        attempt.setUser(user);
        attempt.setCardPhotoHash(cardHash);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getAttemptTtlMinutes()));
        attempt.markFailed(code, reason);
        return attempt;
    }

    private byte[] validateAndExtractBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_IMAGE_SIZE_BYTES);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesImageHeader(bytes, contentType.toLowerCase(Locale.ROOT))) {
                throw new AppException(ErrorCode.INVALID_FILE_TYPE);
            }
            return bytes;
        } catch (IOException e) {
            log.error("Failed to read image bytes: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private boolean matchesImageHeader(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length < 12) return false;
        return switch (contentType) {
            case "image/jpeg" -> (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
            case "image/png" -> java.util.Arrays.equals(java.util.Arrays.copyOf(bytes, 8),
                    new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            case "image/webp" -> new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }

    private String sha256Hex(byte[] bytes) {
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
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

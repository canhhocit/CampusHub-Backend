package com.campushub.modules.identity.controller;

import com.campushub.modules.identity.dto.*;
import com.campushub.modules.identity.service.IdentityVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/attempts")
@Tag(name = "Identity Verification (eKYC)", description = "Step-by-step CCCD, Liveness and Face Verification orchestration APIs")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    public IdentityVerificationController(IdentityVerificationService identityVerificationService) {
        this.identityVerificationService = identityVerificationService;
    }

    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Start eKYC attempt with CCCD card front",
               description = "Upload CCCD front image, extract information via OCR, and create a liveness challenge session")
    public ResponseEntity<StartVerificationResponseDto> startAttempt(
            Authentication authentication,
            @RequestParam("card_file") MultipartFile cardFile) {
        return ResponseEntity.ok(identityVerificationService.startAttempt(authentication.getName(), cardFile));
    }

    @PostMapping(value = "/{attemptId}/liveness-step", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit webcam frames for current liveness challenge",
               description = "Upload frames for current challenge action, verify anti-spoofing and motion completion")
    public ResponseEntity<LivenessStepResultDto> verifyLivenessStep(
            Authentication authentication,
            @PathVariable String attemptId,
            @RequestParam("frames") List<MultipartFile> frames) {
        return ResponseEntity.ok(identityVerificationService.verifyLivenessStep(authentication.getName(), attemptId, frames));
    }

    @PostMapping(value = "/{attemptId}/verify-face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Verify face similarity and complete automated eKYC",
               description = "Upload card front and selfie webcam photo, verify card hash binding and biometric similarity")
    public ResponseEntity<FaceVerificationResultDto> verifyFace(
            Authentication authentication,
            @PathVariable String attemptId,
            @RequestParam("card_file") MultipartFile cardFile,
            @RequestParam("selfie_file") MultipartFile selfieFile) {
        return ResponseEntity.ok(identityVerificationService.verifyFaceAndComplete(
                authentication.getName(), attemptId, cardFile, selfieFile
        ));
    }

    @GetMapping("/{attemptId}")
    @Operation(summary = "Get eKYC attempt status", description = "Query progress, status, and failure code of an attempt")
    public ResponseEntity<IdentityAttemptResponseDto> getAttempt(
            Authentication authentication,
            @PathVariable String attemptId) {
        return ResponseEntity.ok(identityVerificationService.getAttempt(authentication.getName(), attemptId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's latest eKYC attempt", description = "Retrieve latest attempt details for authenticated user")
    public ResponseEntity<IdentityAttemptResponseDto> getMyLatestAttempt(Authentication authentication) {
        return identityVerificationService.getLatestAttempt(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}

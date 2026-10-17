package com.campushub.modules.user.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.user.dto.UpdateProfileRequest;
import com.campushub.modules.user.dto.UserProfileResponse;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private UserProfileResponse mapToProfile(User user) {
        UserProfileResponse res = new UserProfileResponse();
        res.setId(user.getId());
        res.setUsername(user.getUsername());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setRole(user.getRole());
        res.setStatus(user.getStatus());
        res.setDateOfBirth(user.getDateOfBirth());
        boolean isVerified = user.isIdentityApproved();
        res.setIdentityVerified(isVerified);
        res.setIdentityReviewStatus(user.getIdentityReviewStatus());
        res.setCitizenId(user.getCitizenId());
        res.setIdCardFrontUrl(user.getIdCardFrontUrl());
        res.setIdCardBackUrl(user.getIdCardBackUrl());
        res.setIdentityVerifiedAt(user.getIdentityVerifiedAt());
        res.setCreatedAt(user.getCreatedAt());
        return res;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsernameWithLock(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setFullName(request.getFullName().trim());
        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse verifyIdentity(String username, com.campushub.modules.user.dto.VerifyIdentityRequest request) {
        User user = userRepository.findByUsernameWithLock(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isIdentityApproved()) {
            throw new AppException(ErrorCode.ALREADY_IDENTITY_VERIFIED);
        }

        String citizenId = request.getCitizenId().trim();
        if (!citizenId.matches("^[0-9]{12}$")) {
            throw new AppException(ErrorCode.INVALID_CITIZEN_ID);
        }

        userRepository.findByCitizenId(citizenId).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new AppException(ErrorCode.CITIZEN_ID_ALREADY_EXISTS);
            }
        });

        user.setCitizenId(citizenId);
        user.setIdCardFrontUrl(request.getIdCardFrontUrl().trim());
        if (request.getIdCardBackUrl() != null && !request.getIdCardBackUrl().isBlank()) {
            user.setIdCardBackUrl(request.getIdCardBackUrl().trim());
        }
        // Submission is evidence for review, never proof of identity.
        user.setIdentityVerified(false);
        user.setIdentityVerifiedAt(null);
        user.setIdentityReviewedAt(null);

        return mapToProfile(userRepository.save(user));
    }
}

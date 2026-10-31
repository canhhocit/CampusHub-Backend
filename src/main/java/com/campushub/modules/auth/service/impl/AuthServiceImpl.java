package com.campushub.modules.auth.service.impl;

import com.campushub.common.email.EmailService;
import com.campushub.common.enums.Role;
import com.campushub.common.enums.TokenType;
import com.campushub.common.enums.UserStatus;
import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.security.JwtProvider;
import com.campushub.modules.auth.dto.*;
import com.campushub.modules.auth.entity.RefreshToken;
import com.campushub.modules.auth.entity.VerificationToken;
import com.campushub.modules.auth.repository.RefreshTokenRepository;
import com.campushub.modules.auth.repository.VerificationTokenRepository;
import com.campushub.modules.auth.service.AuthService;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;
    private final CacheManager cacheManager;
    private final com.campushub.modules.audit.service.AuditService auditService;
    private final com.campushub.common.security.TokenBlacklistService tokenBlacklistService;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           VerificationTokenRepository verificationTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           EmailService emailService,
                           CacheManager cacheManager,
                           com.campushub.modules.audit.service.AuditService auditService,
                           com.campushub.common.security.TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.emailService = emailService;
        this.cacheManager = cacheManager;
        this.auditService = auditService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    private void checkRateLimit(String cacheName, String key, int maxRequests) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            // get-or-create atomically to avoid two threads both seeing null and both inserting 1
            Cache.ValueWrapper wrapper = cache.get(key);
            AtomicInteger count;
            if (wrapper == null) {
                AtomicInteger newCounter = new AtomicInteger(0);
                Cache.ValueWrapper existing = cache.putIfAbsent(key, newCounter);
                count = (existing == null) ? newCounter : (AtomicInteger) existing.get();
            } else {
                count = (AtomicInteger) wrapper.get();
            }
            if (count != null && count.incrementAndGet() > maxRequests) {
                throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    @Override
    @Transactional
    public void register(RegisterRequest request, HttpServletRequest httpRequest) {
        checkRateLimit("rateLimitRegister", getClientIp(httpRequest), 5);

        if (userRepository.existsByUsername(request.getUsername()) || userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (request.getDateOfBirth() == null) {
            throw new AppException(ErrorCode.AGE_RESTRICTION);
        }
        int age = java.time.Period.between(request.getDateOfBirth(), java.time.LocalDate.now()).getYears();
        if (age < 13 || request.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
            throw new AppException(ErrorCode.AGE_RESTRICTION);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setRole(Role.USER);
        user.setStatus(UserStatus.PENDING);
        userRepository.save(user);

        String rawToken = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setTokenHash(hashToken(rawToken));
        vt.setType(TokenType.EMAIL_VERIFY);
        vt.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(vt);

        String verifyLink = frontendUrl + "/verify-email?token=" + rawToken;
        emailService.sendEmail(user.getEmail(), "Verify your CampusHub account", "Please click the link to verify: " + verifyLink);
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String hashed = hashToken(request.getToken());
        VerificationToken vt = lockVerificationToken(hashed, TokenType.EMAIL_VERIFY);

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByIdWithLock(vt.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        verificationTokenRepository.deleteByUserAndType(user, TokenType.EMAIL_VERIFY);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            TokenResponse response = authenticate(request, httpRequest, httpResponse);
            auditService.logWithUsername(com.campushub.modules.audit.entity.AuditAction.USER_LOGIN,
                    request.getUsernameOrEmail(), true, null);
            return response;
        } catch (AppException ex) {
            auditService.logWithUsername(com.campushub.modules.audit.entity.AuditAction.USER_LOGIN,
                    request.getUsernameOrEmail(), false, ex.getErrorCode().name());
            throw ex;
        }
    }

    private TokenResponse authenticate(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        checkRateLimit("rateLimitLogin", getClientIp(httpRequest), 10);

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new AppException(ErrorCode.USER_PENDING);
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        String accessToken = jwtProvider.generateAccessTokenFromUsernameAndIdAndRole(user.getUsername(), user.getId(), user.getRole().toAuthority());
        setRefreshTokenCookie(user, httpResponse);

        return new TokenResponse(accessToken);
    }

    private void setRefreshTokenCookie(User user, HttpServletResponse response) {
        String rawToken = UUID.randomUUID().toString();
        
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hashToken(rawToken));
        rt.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        refreshTokenRepository.save(rt);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    @Transactional(noRollbackFor = AppException.class)
    public TokenResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String token = getRefreshTokenFromCookies(httpRequest);
        if (token == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String hashed = hashToken(token);
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByTokenHash(hashed);

        if (rtOpt.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        RefreshToken rt = rtOpt.get();

        if (rt.isRevoked()) {
            // Token Reuse Detected!
            refreshTokenRepository.revokeAllUserTokens(rt.getUser());
            throw new AppException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        // Revoke current
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        User user = rt.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        String accessToken = jwtProvider.generateAccessTokenFromUsernameAndIdAndRole(user.getUsername(), user.getId(), user.getRole().toAuthority());
        setRefreshTokenCookie(user, httpResponse);

        return new TokenResponse(accessToken);
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 1. Blacklist Access Token in Redis/In-memory store
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(accessToken);
        }

        // 2. Revoke Refresh Token
        String token = getRefreshTokenFromCookies(httpRequest);
        if (token != null) {
            String hashed = hashToken(token);
            refreshTokenRepository.findByTokenHash(hashed).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }

        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        checkRateLimit("rateLimitForgotPassword", getClientIp(httpRequest), 3);

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            // Return to avoid email enumeration
            return;
        }

        User user = userRepository.findByIdWithLock(userOpt.get().getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);

        String rawToken = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setTokenHash(hashToken(rawToken));
        vt.setType(TokenType.PASSWORD_RESET);
        vt.setExpiresAt(LocalDateTime.now().plusHours(1));
        verificationTokenRepository.save(vt);

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        emailService.sendEmail(user.getEmail(), "Reset your CampusHub password", "Please click the link to reset password: " + resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hashed = hashToken(request.getToken());
        VerificationToken vt = lockVerificationToken(hashed, TokenType.PASSWORD_RESET);

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByIdWithLock(vt.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    private VerificationToken lockVerificationToken(String hash, TokenType type) {
        VerificationToken candidate = verificationTokenRepository.findByTokenHashAndType(hash, type)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
        // Every verification/reset mutation locks the user before token rows.
        // Re-read after waiting: another request may have consumed/deleted the token.
        userRepository.findByIdWithLock(candidate.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
        return verificationTokenRepository.findByTokenHashAndTypeWithLock(hash, type)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
    }

    private String getRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

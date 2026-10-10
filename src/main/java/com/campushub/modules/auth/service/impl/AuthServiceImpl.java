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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           VerificationTokenRepository verificationTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           EmailService emailService,
                           CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.emailService = emailService;
        this.cacheManager = cacheManager;
    }

    private void checkRateLimit(String cacheName, String key, int maxRequests) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            AtomicInteger count = cache.get(key, AtomicInteger.class);
            if (count == null) {
                cache.put(key, new AtomicInteger(1));
            } else if (count.incrementAndGet() > maxRequests) {
                throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip)) {
            ip = request.getRemoteAddr();
        }
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

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.PENDING);
        userRepository.save(user);

        String rawToken = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setTokenHash(hashToken(rawToken));
        vt.setType(TokenType.EMAIL_VERIFY);
        vt.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(vt);

        String verifyLink = "http://localhost:5173/verify-email?token=" + rawToken;
        emailService.sendEmail(user.getEmail(), "Verify your CampusHub account", "Please click the link to verify: " + verifyLink);
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String hashed = hashToken(request.getToken());
        VerificationToken vt = verificationTokenRepository.findByTokenHashAndType(hashed, TokenType.EMAIL_VERIFY)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = vt.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        verificationTokenRepository.deleteByUserAndType(user, TokenType.EMAIL_VERIFY);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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

        String accessToken = jwtProvider.generateAccessTokenFromUsernameAndIdAndRole(user.getUsername(), user.getId(), "ROLE_" + user.getRole().name());
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

        Cookie cookie = new Cookie("refresh_token", rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set to true in production if HTTPS
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge((int) (refreshExpirationMs / 1000));
        response.addCookie(cookie);
    }

    @Override
    @Transactional
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

        String accessToken = jwtProvider.generateAccessTokenFromUsernameAndIdAndRole(user.getUsername(), user.getId(), "ROLE_" + user.getRole().name());
        setRefreshTokenCookie(user, httpResponse);

        return new TokenResponse(accessToken);
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String token = getRefreshTokenFromCookies(httpRequest);
        if (token != null) {
            String hashed = hashToken(token);
            refreshTokenRepository.findByTokenHash(hashed).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }

        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0);
        httpResponse.addCookie(cookie);
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

        User user = userOpt.get();
        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);

        String rawToken = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setTokenHash(hashToken(rawToken));
        vt.setType(TokenType.PASSWORD_RESET);
        vt.setExpiresAt(LocalDateTime.now().plusHours(1));
        verificationTokenRepository.save(vt);

        String resetLink = "http://localhost:5173/reset-password?token=" + rawToken;
        emailService.sendEmail(user.getEmail(), "Reset your CampusHub password", "Please click the link to reset password: " + resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hashed = hashToken(request.getToken());
        VerificationToken vt = verificationTokenRepository.findByTokenHashAndType(hashed, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = vt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        refreshTokenRepository.revokeAllUserTokens(user);
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

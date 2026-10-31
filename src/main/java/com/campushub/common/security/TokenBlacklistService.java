package com.campushub.common.security;

import com.campushub.infrastructure.redis.RedisResilientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

/**
 * Service to manage JWT Access Token revocation and user session invalidation using Redis
 * with in-memory fallback.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private static final String KEY_PREFIX_TOKEN = "blacklist:jwt:";
    private static final String KEY_PREFIX_USER = "blacklist:user:";

    private final RedisResilientService redisService;
    private final JwtProvider jwtProvider;

    public TokenBlacklistService(RedisResilientService redisService, JwtProvider jwtProvider) {
        this.redisService = redisService;
        this.jwtProvider = jwtProvider;
    }

    /**
     * Revokes a single JWT access token until its natural expiration timestamp.
     */
    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Date expiration = jwtProvider.getExpirationDateFromToken(token);
            if (expiration == null) {
                return;
            }
            long diffMillis = expiration.getTime() - System.currentTimeMillis();
            if (diffMillis > 0) {
                String tokenHash = hashToken(token);
                redisService.set(KEY_PREFIX_TOKEN + tokenHash, "revoked", Duration.ofMillis(diffMillis));
                log.info("Blacklisted JWT token (hash: {}...) for {} ms",
                        tokenHash.substring(0, Math.min(10, tokenHash.length())), diffMillis);
            }
        } catch (Exception e) {
            log.warn("Could not parse token for blacklisting: {}", e.getMessage());
        }
    }

    /**
     * Checks if a JWT token has been explicitly revoked via logout or administrative action.
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String tokenHash = hashToken(token);
        return redisService.hasKey(KEY_PREFIX_TOKEN + tokenHash);
    }

    /**
     * Revokes all active sessions for a specific user ID (e.g. on account ban, lock, or password reset).
     */
    public void blacklistUser(Long userId, Duration duration) {
        if (userId == null) {
            return;
        }
        Duration ttl = (duration != null && !duration.isNegative() && !duration.isZero())
                ? duration
                : Duration.ofHours(24);
        redisService.set(KEY_PREFIX_USER + userId, String.valueOf(System.currentTimeMillis()), ttl);
        log.info("Revoked all active sessions for user #{} for {}", userId, ttl);
    }

    /**
     * Checks if a user's tokens issued before a specific timestamp have been revoked.
     */
    public boolean isUserRevoked(Long userId, Date tokenIssuedAt) {
        if (userId == null || tokenIssuedAt == null) {
            return false;
        }
        String revokedAtStr = redisService.get(KEY_PREFIX_USER + userId);
        if (revokedAtStr != null) {
            try {
                long revokedAt = Long.parseLong(revokedAtStr);
                return tokenIssuedAt.getTime() < revokedAt;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(token.hashCode());
        }
    }
}


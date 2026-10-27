package com.campushub.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiration}")
    private long jwtAccessExpirationMs;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @jakarta.annotation.PostConstruct
    public void validateJwtConfiguration() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured via environment variable JWT_SECRET or application configuration.");
        }
        byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) long for HMAC-SHA256 security.");
        }
        boolean isProduction = "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
        if (isProduction && "super_secret_key_change_in_production_min_256_bits_length_for_hmac_sha256".equals(jwtSecret)) {
            throw new IllegalStateException("Insecure default JWT secret cannot be used in production profile! Set JWT_SECRET in environment.");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return Jwts.builder()
                .subject((userPrincipal.getUsername()))
                .claim("id", userPrincipal.getId())
                .claim("role", userPrincipal.getAuthorities().iterator().next().getAuthority())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtAccessExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String generateAccessTokenFromUsernameAndIdAndRole(String username, Long id, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtAccessExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}

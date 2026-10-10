package com.campushub.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtSecurityTest {

    @Test
    public void testValidSecretPassesValidation() {
        JwtProvider jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "valid_secret_key_that_is_at_least_32_bytes_long!");
        ReflectionTestUtils.setField(jwtProvider, "jwtAccessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtProvider, "activeProfile", "dev");

        assertDoesNotThrow(jwtProvider::validateJwtConfiguration);
    }

    @Test
    public void testShortSecretFailsValidation() {
        JwtProvider jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "short_secret");
        ReflectionTestUtils.setField(jwtProvider, "jwtAccessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtProvider, "activeProfile", "dev");

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProvider::validateJwtConfiguration);
        assertTrue(ex.getMessage().contains("at least 256 bits (32 bytes)"));
    }

    @Test
    public void testDefaultSecretFailsInProductionProfile() {
        JwtProvider jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "super_secret_key_change_in_production_min_256_bits_length_for_hmac_sha256");
        ReflectionTestUtils.setField(jwtProvider, "jwtAccessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtProvider, "activeProfile", "prod");

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProvider::validateJwtConfiguration);
        assertTrue(ex.getMessage().contains("Insecure default JWT secret cannot be used in production profile"));
    }

    @Test
    public void testNullOrEmptySecretFailsValidation() {
        JwtProvider jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "");
        ReflectionTestUtils.setField(jwtProvider, "jwtAccessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtProvider, "activeProfile", "dev");

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProvider::validateJwtConfiguration);
        assertTrue(ex.getMessage().contains("JWT secret must be configured"));
    }
}

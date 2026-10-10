package com.campushub.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AesEncryptionServiceTest {

    private final AesEncryptionService encryptionService = new AesEncryptionService("TestSecretEncryptionKeyForJUnit32B!");

    @Test
    public void testEncryptAndDecryptSuccess() {
        String originalPin = "839201";
        String encrypted = encryptionService.encrypt(originalPin);

        assertNotNull(encrypted);
        assertNotEquals(originalPin, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(originalPin, decrypted);
    }

    @Test
    public void testDecryptInvalidDataReturnsNull() {
        assertNull(encryptionService.decrypt(null));
        assertNull(encryptionService.decrypt(""));
        assertNull(encryptionService.decrypt("not-a-valid-base64-or-ciphertext"));
    }

    @Test
    public void testEncryptionProducesDifferentCiphertextsForSamePlaintext() {
        String pin = "123456";
        String cipher1 = encryptionService.encrypt(pin);
        String cipher2 = encryptionService.encrypt(pin);

        // Different IVs mean different ciphertexts
        assertNotEquals(cipher1, cipher2);
        // Both decrypt back to same plaintext
        assertEquals(pin, encryptionService.decrypt(cipher1));
        assertEquals(pin, encryptionService.decrypt(cipher2));
    }
}

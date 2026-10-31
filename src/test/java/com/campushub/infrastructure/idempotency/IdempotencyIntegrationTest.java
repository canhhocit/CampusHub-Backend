package com.campushub.infrastructure.idempotency;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
public class IdempotencyIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        // Clean test keys
        idempotencyService.release("test:op:1");
        idempotencyService.release("test:op:2");
        idempotencyService.release("payos:webhook:99999");
    }

    @Test
    @DisplayName("acquireOrThrow succeeds first time, throws IDEMPOTENT_OPERATION_IN_PROGRESS second time, succeeds after release")
    void testAcquireOrThrowLifecycle() {
        String key = "test:op:1";

        // 1. First acquire should succeed
        assertDoesNotThrow(() -> idempotencyService.acquireOrThrow(key, Duration.ofSeconds(30)));

        // 2. Second concurrent acquire should fail with IDEMPOTENT_OPERATION_IN_PROGRESS
        AppException ex = assertThrows(AppException.class,
                () -> idempotencyService.acquireOrThrow(key, Duration.ofSeconds(30)));
        assertEquals(ErrorCode.IDEMPOTENT_OPERATION_IN_PROGRESS, ex.getErrorCode());

        // 3. Release lock
        idempotencyService.release(key);

        // 4. Acquire should succeed again after release
        assertDoesNotThrow(() -> idempotencyService.acquireOrThrow(key, Duration.ofSeconds(30)));

        // Clean up
        idempotencyService.release(key);
    }

    @Test
    @DisplayName("tryAcquire returns true on first call, false on second call until release or completion")
    void testTryAcquireAndCompletion() {
        String key = "payos:webhook:99999";

        // 1. First tryAcquire returns true
        assertTrue(idempotencyService.tryAcquire(key, Duration.ofMinutes(5)));

        // 2. Subsequent tryAcquire returns false (already in progress)
        assertFalse(idempotencyService.tryAcquire(key, Duration.ofMinutes(5)));

        // 3. Mark completed
        idempotencyService.markCompleted(key, Duration.ofMinutes(5));
        assertTrue(idempotencyService.isCompleted(key));

        // 4. While completed and not expired, tryAcquire still returns false
        assertFalse(idempotencyService.tryAcquire(key, Duration.ofMinutes(5)));

        // Clean up
        idempotencyService.release(key);
        assertFalse(idempotencyService.isCompleted(key));
    }
}


package com.campushub.infrastructure.idempotency;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.redis.RedisResilientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service to ensure idempotent financial and webhook operations, preventing duplicate
 * transactions from double-clicks or repeated network retries.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";

    private final RedisResilientService redisService;

    public IdempotencyService(RedisResilientService redisService) {
        this.redisService = redisService;
    }

    /**
     * Tries to acquire an idempotency lock for the given key.
     * Throws AppException(ErrorCode.IDEMPOTENT_OPERATION_IN_PROGRESS) if already in progress.
     *
     * @param operationKey Unique key identifying the transaction (e.g. "payos:webhook:12345" or "withdraw:user:99")
     * @param lockTtl Maximum time the operation lock should be held
     */
    public void acquireOrThrow(String operationKey, Duration lockTtl) {
        String fullKey = KEY_PREFIX + operationKey;
        boolean acquired = redisService.setIfAbsent(fullKey, "PROCESSING", lockTtl);
        if (!acquired) {
            String currentStatus = redisService.get(fullKey);
            log.warn("Duplicate request detected for idempotency key [{}] (status: {})", operationKey, currentStatus);
            throw new AppException(ErrorCode.IDEMPOTENT_OPERATION_IN_PROGRESS,
                    "Yêu cầu tương tự đang được hệ thống xử lý hoặc đã hoàn tất. Vui lòng không gửi lại.");
        }
        log.debug("Acquired idempotency lock for [{}] with TTL {}", operationKey, lockTtl);
    }

    /**
     * Tries to acquire an idempotency lock without throwing an exception.
     * Returns true if successfully acquired, false if key already exists.
     */
    public boolean tryAcquire(String operationKey, Duration lockTtl) {
        String fullKey = KEY_PREFIX + operationKey;
        return redisService.setIfAbsent(fullKey, "PROCESSING", lockTtl);
    }

    /**
     * Marks the operation as successfully completed, retaining the key for a cooldown period
     * to prevent immediate replays.
     */
    public void markCompleted(String operationKey, Duration retainTtl) {
        String fullKey = KEY_PREFIX + operationKey;
        redisService.set(fullKey, "COMPLETED", retainTtl);
        log.debug("Marked idempotency key [{}] as COMPLETED (retain: {})", operationKey, retainTtl);
    }

    /**
     * Releases/deletes the lock so that the client can retry in case of a business/recoverable error.
     */
    public void release(String operationKey) {
        String fullKey = KEY_PREFIX + operationKey;
        redisService.delete(fullKey);
        log.debug("Released idempotency lock for [{}]", operationKey);
    }

    /**
     * Checks if the operation was already completed previously.
     */
    public boolean isCompleted(String operationKey) {
        String fullKey = KEY_PREFIX + operationKey;
        return "COMPLETED".equals(redisService.get(fullKey));
    }
}


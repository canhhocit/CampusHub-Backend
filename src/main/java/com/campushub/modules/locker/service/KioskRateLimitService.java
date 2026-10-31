package com.campushub.modules.locker.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.redis.RedisResilientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service to protect Smart Locker Kiosk terminals from PIN brute-force and spam attacks.
 */
@Service
public class KioskRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(KioskRateLimitService.class);

    private static final String KEY_PREFIX_LOCKER = "kiosk:ratelimit:failed:locker:";
    private static final String KEY_PREFIX_IP = "kiosk:ratelimit:failed:ip:";

    private final RedisResilientService redisService;
    private final int maxFailedAttempts;
    private final int lockoutDurationMinutes;

    public KioskRateLimitService(
            RedisResilientService redisService,
            @Value("${app.kiosk.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.kiosk.lockout-duration-minutes:10}") int lockoutDurationMinutes
    ) {
        this.redisService = redisService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    /**
     * Checks if the current terminal or client IP has exceeded the allowed failed PIN attempts.
     * Throws AppException(ErrorCode.KIOSK_RATE_LIMIT_EXCEEDED) if rate limited.
     */
    public void checkRateLimit(Long lockerId, String clientIp) {
        if (lockerId != null) {
            String lockerKey = KEY_PREFIX_LOCKER + lockerId;
            String lockerAttemptsStr = redisService.get(lockerKey);
            if (lockerAttemptsStr != null) {
                try {
                    int attempts = Integer.parseInt(lockerAttemptsStr);
                    if (attempts >= maxFailedAttempts) {
                        log.warn("Kiosk terminal #{} is rate-limited ({} failed attempts >= max {})",
                                lockerId, attempts, maxFailedAttempts);
                        throw new AppException(ErrorCode.KIOSK_RATE_LIMIT_EXCEEDED,
                                String.format("Bạn đã nhập sai mã PIN quá %d lần. Trạm tủ tạm thời bị khóa thử lại trong %d phút để bảo vệ tài sản.",
                                        maxFailedAttempts, lockoutDurationMinutes));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (clientIp != null && !clientIp.isBlank()) {
            String ipKey = KEY_PREFIX_IP + clientIp.trim();
            String ipAttemptsStr = redisService.get(ipKey);
            if (ipAttemptsStr != null) {
                try {
                    int attempts = Integer.parseInt(ipAttemptsStr);
                    if (attempts >= maxFailedAttempts) {
                        log.warn("Client IP {} is rate-limited at kiosk ({} failed attempts >= max {})",
                                clientIp, attempts, maxFailedAttempts);
                        throw new AppException(ErrorCode.KIOSK_RATE_LIMIT_EXCEEDED,
                                String.format("Địa chỉ mạng của bạn đã nhập sai mã PIN quá %d lần. Vui lòng thử lại sau %d phút.",
                                        maxFailedAttempts, lockoutDurationMinutes));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * Records a failed PIN attempt, incrementing counter with the configured lockout TTL.
     */
    public void recordFailedAttempt(Long lockerId, String clientIp) {
        Duration ttl = Duration.ofMinutes(lockoutDurationMinutes);
        if (lockerId != null) {
            long attempts = redisService.increment(KEY_PREFIX_LOCKER + lockerId, ttl);
            log.warn("Recorded failed PIN attempt #{} for locker terminal #{}", attempts, lockerId);
        }
        if (clientIp != null && !clientIp.isBlank()) {
            long ipAttempts = redisService.increment(KEY_PREFIX_IP + clientIp.trim(), ttl);
            log.warn("Recorded failed PIN attempt #{} for IP {}", ipAttempts, clientIp);
        }
    }

    /**
     * Resets the failed attempts counter upon a successful PIN entry.
     */
    public void resetFailedAttempts(Long lockerId, String clientIp) {
        if (lockerId != null) {
            redisService.delete(KEY_PREFIX_LOCKER + lockerId);
        }
        if (clientIp != null && !clientIp.isBlank()) {
            redisService.delete(KEY_PREFIX_IP + clientIp.trim());
        }
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }
}


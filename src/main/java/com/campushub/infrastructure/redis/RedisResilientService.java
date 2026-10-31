package com.campushub.infrastructure.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resilient Redis service providing high-performance key-value operations with
 * automatic graceful degradation to an in-memory store when Redis is unavailable or disabled.
 */
@Service
public class RedisResilientService {

    private static final Logger log = LoggerFactory.getLogger(RedisResilientService.class);

    private final Optional<StringRedisTemplate> redisTemplate;
    private final boolean redisEnabled;
    private final AtomicBoolean loggedFallbackWarning = new AtomicBoolean(false);

    // In-memory fallback structures for zero-downtime and offline testing
    private final ConcurrentHashMap<String, InMemoryEntry> memoryStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InMemoryCounter> counterStore = new ConcurrentHashMap<>();

    private record InMemoryEntry(String value, long expireAtMillis) {
        boolean isExpired() {
            return expireAtMillis > 0 && System.currentTimeMillis() > expireAtMillis;
        }
    }

    private record InMemoryCounter(AtomicLong value, long expireAtMillis) {
        boolean isExpired() {
            return expireAtMillis > 0 && System.currentTimeMillis() > expireAtMillis;
        }
    }

    @Autowired
    public RedisResilientService(
            Optional<StringRedisTemplate> redisTemplate,
            @Value("${app.redis.enabled:true}") boolean redisEnabled
    ) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;

        if (!redisEnabled) {
            log.info("Redis is explicitly disabled (app.redis.enabled=false). Operating in local in-memory fallback mode.");
        }
    }

    public boolean isRedisAvailable() {
        if (!redisEnabled || redisTemplate.isEmpty()) {
            return false;
        }
        try {
            String pingResult = redisTemplate.get().getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pingResult);
        } catch (Exception e) {
            return false;
        }
    }

    public String get(String key) {
        if (redisEnabled && redisTemplate.isPresent()) {
            try {
                return redisTemplate.get().opsForValue().get(key);
            } catch (Exception e) {
                handleRedisException("get", key, e);
            }
        }
        InMemoryEntry entry = memoryStore.get(key);
        if (entry != null) {
            if (entry.isExpired()) {
                memoryStore.remove(key);
                return null;
            }
            return entry.value();
        }
        InMemoryCounter counter = counterStore.get(key);
        if (counter != null) {
            if (counter.isExpired()) {
                counterStore.remove(key);
                return null;
            }
            return String.valueOf(counter.value().get());
        }
        return null;
    }

    public void set(String key, String value, Duration ttl) {
        if (redisEnabled && redisTemplate.isPresent()) {
            try {
                if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.get().opsForValue().set(key, value, ttl);
                } else {
                    redisTemplate.get().opsForValue().set(key, value);
                }
                return;
            } catch (Exception e) {
                handleRedisException("set", key, e);
            }
        }
        long expireAt = (ttl != null && !ttl.isNegative() && !ttl.isZero())
                ? System.currentTimeMillis() + ttl.toMillis()
                : 0L;
        memoryStore.put(key, new InMemoryEntry(value, expireAt));
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        if (redisEnabled && redisTemplate.isPresent()) {
            try {
                Boolean success;
                if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
                    success = redisTemplate.get().opsForValue().setIfAbsent(key, value, ttl);
                } else {
                    success = redisTemplate.get().opsForValue().setIfAbsent(key, value);
                }
                if (success != null) {
                    return success;
                }
            } catch (Exception e) {
                handleRedisException("setIfAbsent", key, e);
            }
        }

        long now = System.currentTimeMillis();
        long expireAt = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? now + ttl.toMillis() : 0L;

        InMemoryEntry existing = memoryStore.get(key);
        if (existing == null || existing.isExpired()) {
            memoryStore.put(key, new InMemoryEntry(value, expireAt));
            return true;
        }
        return false;
    }

    public long increment(String key, Duration ttl) {
        if (redisEnabled && redisTemplate.isPresent()) {
            try {
                Long val = redisTemplate.get().opsForValue().increment(key);
                if (val != null) {
                    if (val == 1 && ttl != null && !ttl.isNegative() && !ttl.isZero()) {
                        redisTemplate.get().expire(key, ttl);
                    }
                    return val;
                }
            } catch (Exception e) {
                handleRedisException("increment", key, e);
            }
        }

        long now = System.currentTimeMillis();
        long expireAt = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? now + ttl.toMillis() : 0L;

        InMemoryCounter counter = counterStore.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new InMemoryCounter(new AtomicLong(1), expireAt);
            }
            existing.value().incrementAndGet();
            return existing;
        });

        return counter.value().get();
    }

    public boolean delete(String key) {
        boolean removedFromMemory = memoryStore.remove(key) != null || counterStore.remove(key) != null;
        if (redisEnabled && redisTemplate.isPresent()) {
            try {
                Boolean redisDeleted = redisTemplate.get().delete(key);
                return Boolean.TRUE.equals(redisDeleted) || removedFromMemory;
            } catch (Exception e) {
                handleRedisException("delete", key, e);
            }
        }
        return removedFromMemory;
    }

    public boolean hasKey(String key) {
        if (redisEnabled && redisTemplate.isPresent()) {
            try {
                Boolean exists = redisTemplate.get().hasKey(key);
                if (Boolean.TRUE.equals(exists)) {
                    return true;
                }
            } catch (Exception e) {
                handleRedisException("hasKey", key, e);
            }
        }
        InMemoryEntry entry = memoryStore.get(key);
        if (entry != null) {
            if (entry.isExpired()) {
                memoryStore.remove(key);
                return false;
            }
            return true;
        }
        InMemoryCounter counter = counterStore.get(key);
        if (counter != null) {
            if (counter.isExpired()) {
                counterStore.remove(key);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Clears all in-memory fallback entries (useful for testing).
     */
    public void clearMemoryStore() {
        memoryStore.clear();
        counterStore.clear();
    }

    private void handleRedisException(String operation, String key, Exception e) {
        if (loggedFallbackWarning.compareAndSet(false, true)) {
            log.warn("Redis operation [{}] on key [{}] failed ({}). Gracefully falling back to in-memory store.",
                    operation, key, e.getMessage());
        } else {
            log.debug("Redis operation [{}] on key [{}] failed: {}. Used in-memory fallback.",
                    operation, key, e.getMessage());
        }
    }
}

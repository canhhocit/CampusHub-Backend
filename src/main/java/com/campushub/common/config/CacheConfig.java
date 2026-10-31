package com.campushub.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private static final List<String> CACHE_NAMES = List.of(
            "rateLimitLogin",
            "rateLimitRegister",
            "rateLimitForgotPassword",
            "categories",
            "lockers",
            "system_settings"
    );

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(10000);
    }

    @Bean
    @Primary
    public CacheManager cacheManager(
            Caffeine<Object, Object> caffeine,
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            Optional<RedisConnectionFactory> redisConnectionFactory
    ) {
        if (redisEnabled && redisConnectionFactory.isPresent()) {
            try {
                String ping = redisConnectionFactory.get().getConnection().ping();
                if ("PONG".equalsIgnoreCase(ping)) {
                    log.info("Redis is reachable (PONG). Configuring RedisCacheManager for distributed caching.");

                    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofMinutes(15))
                            .disableCachingNullValues()
                            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

                    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
                    cacheConfigs.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(15)));
                    cacheConfigs.put("lockers", defaultConfig.entryTtl(Duration.ofMinutes(10)));
                    cacheConfigs.put("system_settings", defaultConfig.entryTtl(Duration.ofMinutes(30)));
                    cacheConfigs.put("rateLimitLogin", defaultConfig.entryTtl(Duration.ofMinutes(15)));
                    cacheConfigs.put("rateLimitRegister", defaultConfig.entryTtl(Duration.ofMinutes(15)));
                    cacheConfigs.put("rateLimitForgotPassword", defaultConfig.entryTtl(Duration.ofMinutes(15)));

                    return RedisCacheManager.builder(redisConnectionFactory.get())
                            .cacheDefaults(defaultConfig)
                            .withInitialCacheConfigurations(cacheConfigs)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Redis ping failed during CacheManager bootstrap ({}). Gracefully falling back to Caffeine cache.", e.getMessage());
            }
        }

        log.info("Configuring CaffeineCacheManager (in-memory mode for development/offline).");
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        caffeineCacheManager.setCacheNames(CACHE_NAMES);
        return caffeineCacheManager;
    }
}

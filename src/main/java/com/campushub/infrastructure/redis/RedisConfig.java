package com.campushub.infrastructure.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(
            RedisProperties properties,
            @Value("${REDIS_URL:}") String redisUrl,
            @Value("${REDIS_SSL:false}") boolean redisSsl,
            @Value("${app.redis.enabled:true}") boolean redisEnabled) {

        if (!redisEnabled) {
            log.info("Redis is disabled via app.redis.enabled=false. Initializing offline stub connection factory.");
            return new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        }

        Duration timeout = properties.getTimeout() != null ? properties.getTimeout() : Duration.ofMillis(5000);

        // Case 1: Connect via REDIS_URL if provided
        if (redisUrl != null && !redisUrl.isBlank()) {
            boolean useSsl = redisUrl.startsWith("rediss://") || redisSsl || redisUrl.contains("upstash.io");
            String normalizedUrl = redisUrl.trim();
            if (useSsl && normalizedUrl.startsWith("redis://")) {
                normalizedUrl = "rediss://" + normalizedUrl.substring("redis://".length());
            }

            log.info("Configuring LettuceConnectionFactory via REDIS_URL (SSL: {})", useSsl);
            RedisConfiguration redisConfig = LettuceConnectionFactory.createRedisConfiguration(normalizedUrl);
            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder = LettuceClientConfiguration.builder();
            if (useSsl) {
                clientBuilder.useSsl();
            }
            clientBuilder.commandTimeout(timeout);
            LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientBuilder.build());
            factory.afterPropertiesSet();
            return factory;
        }

        // Case 2: Connect via host/port/username/password/ssl properties
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(properties.getHost() != null ? properties.getHost() : "localhost");
        standalone.setPort(properties.getPort() > 0 ? properties.getPort() : 6379);

        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            standalone.setUsername(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            standalone.setPassword(RedisPassword.of(properties.getPassword()));
        }

        boolean useSsl = (properties.getSsl() != null && properties.getSsl().isEnabled())
                || redisSsl
                || (properties.getHost() != null && properties.getHost().contains("upstash.io"));

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder = LettuceClientConfiguration.builder();
        if (useSsl) {
            clientBuilder.useSsl();
        }
        clientBuilder.commandTimeout(timeout);

        log.info("Configuring LettuceConnectionFactory for host {}:{} (username: {}, SSL: {})",
                standalone.getHostName(), standalone.getPort(), standalone.getUsername(), useSsl);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientBuilder.build());
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}

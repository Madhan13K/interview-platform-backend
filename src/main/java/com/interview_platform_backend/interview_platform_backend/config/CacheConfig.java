package com.interview_platform_backend.interview_platform_backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    private Jackson2JsonRedisSerializer<Object> jsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS,
                JsonTypeInfo.As.PROPERTY
        );
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Object> serializer = jsonRedisSerializer();
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // Short TTL caches (frequently changing data)
        cacheConfigurations.put("interviews", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("interviewsByCandidate", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("interviewsByInterviewer", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("notifications", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("dashboard", defaultConfig.entryTtl(Duration.ofMinutes(3)));

        // Medium TTL caches
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("userProfiles", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("jobPositions", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("organizations", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("webhooks", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Long TTL caches (rarely changing data)
        cacheConfigurations.put("roles", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("permissions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("rolePermissions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("templates", defaultConfig.entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("questions", defaultConfig.entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("questionCategories", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("tags", defaultConfig.entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("pipelines", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Token/session caches
        cacheConfigurations.put("rateLimits", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("tokenBlacklist", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean("caffeineCacheManager")
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "none")
    @Primary
    public CacheManager caffeineCacheManagerPrimary() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        return cacheManager;
    }

    @Bean("caffeineCacheManagerLocal")
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "localRoles", "localPermissions"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Object> serializer = jsonRedisSerializer();
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}

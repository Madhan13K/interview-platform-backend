package com.interview_platform_backend.interview_platform_backend.security.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRateLimiterService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if a request should be rate limited.
     * Uses Redis INCR + EXPIRE for sliding window rate limiting.
     *
     * @param key unique key (e.g., "rate:login:192.168.1.1")
     * @param maxRequests maximum requests allowed in the window
     * @param windowSeconds time window in seconds
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = "ratelimit:" + key;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        return currentCount != null && currentCount <= maxRequests;
    }

    /**
     * Get remaining requests for a key.
     */
    public long getRemainingRequests(String key, int maxRequests) {
        String redisKey = "ratelimit:" + key;
        Object count = redisTemplate.opsForValue().get(redisKey);
        if (count == null) return maxRequests;
        long current = count instanceof Number ? ((Number) count).longValue() : 0;
        return Math.max(0, maxRequests - current);
    }

    /**
     * Add a token to the blacklist (for logout/revocation).
     */
    public void blacklistToken(String tokenId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                "token:blacklist:" + tokenId, "revoked", ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Check if a token is blacklisted.
     */
    public boolean isTokenBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + tokenId));
    }
}

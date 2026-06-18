package com.interview_platform_backend.interview_platform_backend.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter that uses Redis as primary store with in-memory fallback.
 * <ul>
 *   <li>Auth endpoints (login/register/forgot-password): 5 requests/minute per IP (brute-force protection)</li>
 *   <li>General API endpoints: 60 requests/minute per authenticated user, 30/minute per anonymous IP</li>
 * </ul>
 * If Redis is unavailable, falls back to ConcurrentHashMap-based in-memory limiting.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Endpoint-specific limits
    private static final int AUTH_LOGIN_LIMIT = 5;
    private static final int AUTH_REGISTER_LIMIT = 10;
    private static final int AUTH_FORGOT_PASSWORD_LIMIT = 3;
    private static final int AUTH_DEFAULT_LIMIT = 10;
    private static final int AUTHENTICATED_USER_LIMIT = 60;
    private static final int ANONYMOUS_LIMIT = 30;

    private static final int WINDOW_SECONDS = 60;
    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final long EXPIRY_MS = 120_000; // 2 minutes - entries older than this are cleaned up
    private static final int MAX_ENTRIES = 100_000;

    private final RedisRateLimiterService redisRateLimiterService;
    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(RedisRateLimiterService redisRateLimiterService) {
        this.redisRateLimiterService = redisRateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        String userIdentifier = resolveUserIdentifier();

        int limit = resolveLimit(path, userIdentifier);
        String key = buildKey(path, userIdentifier, clientIp);

        boolean allowed;
        long remaining;

        try {
            // Try Redis first
            allowed = redisRateLimiterService.isAllowed(key, limit, WINDOW_SECONDS);
            remaining = redisRateLimiterService.getRemainingRequests(key, limit);
        } catch (Exception e) {
            // Redis unavailable, fall back to in-memory
            log.debug("Redis unavailable for rate limiting, falling back to in-memory: {}", e.getMessage());
            InMemoryRateLimitResult result = checkInMemory(key, limit);
            allowed = result.allowed;
            remaining = result.remaining;
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}
                    """);
            return;
        }

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));

        filterChain.doFilter(request, response);
    }

    /**
     * In-memory fallback when Redis is unavailable.
     */
    private InMemoryRateLimitResult checkInMemory(String key, int limit) {
        // DoS protection: if map is too large, clear it entirely
        if (requestCounts.size() > MAX_ENTRIES) {
            log.warn("Rate limit map exceeded {} entries, clearing entirely for DoS protection", MAX_ENTRIES);
            requestCounts.clear();
        }

        RateLimitEntry entry = requestCounts.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        boolean allowed = entry.count.get() <= limit;
        long remaining = Math.max(0, limit - entry.count.get());
        return new InMemoryRateLimitResult(allowed, remaining);
    }

    /**
     * Scheduled cleanup that removes entries older than 2 minutes.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        int sizeBefore = requestCounts.size();

        requestCounts.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > EXPIRY_MS
        );

        int removed = sizeBefore - requestCounts.size();
        if (removed > 0) {
            log.debug("Rate limiter cleanup: removed {} expired entries, {} remaining",
                    removed, requestCounts.size());
        }
    }

    /**
     * Resolve the rate limit based on endpoint and user type.
     */
    private int resolveLimit(String path, String userIdentifier) {
        if (path.startsWith("/api/v1/auth/")) {
            // Per-endpoint tuning for auth paths
            if (path.contains("/login")) {
                return AUTH_LOGIN_LIMIT;
            } else if (path.contains("/register")) {
                return AUTH_REGISTER_LIMIT;
            } else if (path.contains("/forgot-password") || path.contains("/reset-password")) {
                return AUTH_FORGOT_PASSWORD_LIMIT;
            }
            return AUTH_DEFAULT_LIMIT;
        }

        // For non-auth endpoints: higher limit for authenticated users
        if (userIdentifier != null) {
            return AUTHENTICATED_USER_LIMIT;
        }
        return ANONYMOUS_LIMIT;
    }

    /**
     * Build a rate-limit key combining identity and endpoint category.
     */
    private String buildKey(String path, String userIdentifier, String clientIp) {
        String identity = (userIdentifier != null) ? "user:" + userIdentifier : "ip:" + clientIp;

        if (path.startsWith("/api/v1/auth/")) {
            // For auth endpoints, use IP only (users are not authenticated yet)
            return "ip:" + clientIp + ":" + extractAuthEndpoint(path);
        }
        // For general endpoints, group by identity
        return identity + ":general";
    }

    private String extractAuthEndpoint(String path) {
        if (path.contains("/login")) return "login";
        if (path.contains("/register")) return "register";
        if (path.contains("/forgot-password")) return "forgot-password";
        if (path.contains("/reset-password")) return "reset-password";
        if (path.contains("/verify-email")) return "verify-email";
        if (path.contains("/resend-verification")) return "resend-verification";
        return "auth-other";
    }

    private String resolveUserIdentifier() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    private record InMemoryRateLimitResult(boolean allowed, long remaining) {
    }
}

package com.interview_platform_backend.interview_platform_backend.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates MDC (Mapped Diagnostic Context) with correlation IDs
 * and request metadata for structured logging.
 * 
 * MDC fields set:
 * - correlationId: Unique request ID (from X-Correlation-ID header or auto-generated)
 * - traceId: OpenTelemetry trace ID (from traceparent header)
 * - spanId: OpenTelemetry span ID
 * - requestMethod: HTTP method (GET, POST, etc.)
 * - requestUri: Request URI path
 * - clientIp: Client IP address (X-Forwarded-For aware)
 * - userAgent: User-Agent header
 * - userId: Authenticated user's email/username
 * 
 * These fields are automatically included in all log entries within the request scope.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String TRACE_PARENT_HEADER = "traceparent";

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_REQUEST_METHOD = "requestMethod";
    public static final String MDC_REQUEST_URI = "requestUri";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_USER_AGENT = "userAgent";
    public static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Set correlation ID (use existing header or generate new)
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_CORRELATION_ID, correlationId);

            // Parse OpenTelemetry trace context if present
            String traceparent = request.getHeader(TRACE_PARENT_HEADER);
            if (traceparent != null && !traceparent.isBlank()) {
                parseTraceparent(traceparent);
            }

            // Set request metadata
            MDC.put(MDC_REQUEST_METHOD, request.getMethod());
            MDC.put(MDC_REQUEST_URI, request.getRequestURI());
            MDC.put(MDC_CLIENT_IP, extractClientIp(request));

            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                MDC.put(MDC_USER_AGENT, userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent);
            }

            // Set response header so clients can track their request
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue filter chain
            filterChain.doFilter(request, response);

            // After authentication, add user ID if available
            populateUserId();

        } finally {
            // Always clear MDC to prevent leaking between requests (especially with thread pools)
            MDC.clear();
        }
    }

    /**
     * Parse W3C Trace Context traceparent header.
     * Format: {version}-{traceId}-{spanId}-{traceFlags}
     * Example: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
     */
    private void parseTraceparent(String traceparent) {
        try {
            String[] parts = traceparent.split("-");
            if (parts.length >= 4) {
                MDC.put(MDC_TRACE_ID, parts[1]);
                MDC.put(MDC_SPAN_ID, parts[2]);
            }
        } catch (Exception e) {
            // Ignore malformed traceparent
        }
    }

    private void populateUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(MDC_USER_ID, auth.getName());
            }
        } catch (Exception e) {
            // Ignore - security context may not be available
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip correlation for static resources and health checks
        return path.startsWith("/actuator/health") ||
               path.startsWith("/favicon.ico");
    }
}

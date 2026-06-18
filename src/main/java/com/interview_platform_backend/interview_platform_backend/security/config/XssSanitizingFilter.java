package com.interview_platform_backend.interview_platform_backend.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that sanitizes request parameters and headers to prevent XSS attacks.
 * Strips potentially dangerous HTML/script content from input values.
 */
@Component
public class XssSanitizingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        XssSanitizingRequestWrapper wrappedRequest = new XssSanitizingRequestWrapper(request);
        filterChain.doFilter(wrappedRequest, response);
    }
}

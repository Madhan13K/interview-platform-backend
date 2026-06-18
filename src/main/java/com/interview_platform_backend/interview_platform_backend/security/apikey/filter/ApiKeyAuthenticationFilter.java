package com.interview_platform_backend.interview_platform_backend.security.apikey.filter;

import com.interview_platform_backend.interview_platform_backend.security.apikey.entity.ApiKey;
import com.interview_platform_backend.interview_platform_backend.security.apikey.service.ApiKeyService;
import com.interview_platform_backend.interview_platform_backend.security.jwt.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;
    private final CustomUserDetailsService customUserDetailsService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService,
                                      CustomUserDetailsService customUserDetailsService) {
        this.apiKeyService = apiKeyService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKeyHeader = request.getHeader(API_KEY_HEADER);

        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            // No API key header, pass through to let JWT filter handle
            filterChain.doFilter(request, response);
            return;
        }

        // Already authenticated (e.g., by another filter)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiKey apiKey = apiKeyService.validateApiKey(apiKeyHeader);

        if (apiKey == null) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or expired API key\"}");
            return;
        }

        // Load user details for the API key owner
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(apiKey.getUser().getEmail());

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}

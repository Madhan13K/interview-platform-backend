package com.interview_platform_backend.interview_platform_backend.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Custom authorization request resolver that forces PKCE (S256) on ALL OAuth2
 * authorization requests — regardless of whether the client is confidential or public.
 *
 * <p>This ensures that even if a confidential client is used as a BFF for an SPA,
 * the authorization code exchange is protected against interception attacks.
 *
 * <p>Spring Security already supports PKCE natively for public clients
 * (client-authentication-method: none), but this resolver extends it to
 * ALL registrations for defense-in-depth.
 */
@Component
public class PkceAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public PkceAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");

        // Force PKCE (code_challenge + code_challenge_method=S256) on every request
        this.defaultResolver.setAuthorizationRequestCustomizer(
                OAuth2AuthorizationRequestCustomizers.withPkce()
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return defaultResolver.resolve(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return defaultResolver.resolve(request, clientRegistrationId);
    }
}

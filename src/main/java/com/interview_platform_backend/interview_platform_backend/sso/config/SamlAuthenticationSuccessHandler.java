package com.interview_platform_backend.interview_platform_backend.sso.config;

import com.interview_platform_backend.interview_platform_backend.sso.service.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Handles successful SAML2 authentication by extracting user attributes,
 * provisioning users if needed, generating JWT tokens, and redirecting
 * to the frontend with tokens.
 */
@Component
public class SamlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(SamlAuthenticationSuccessHandler.class);

    private final SsoService ssoService;

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String frontendRedirectUri;

    public SamlAuthenticationSuccessHandler(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof Saml2Authentication saml2Auth)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid SAML authentication");
            return;
        }

        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) saml2Auth.getPrincipal();
        String registrationId = saml2Auth.getSaml2Response() != null
                ? extractRegistrationId(request)
                : "unknown";

        // Extract attributes from SAML assertion
        String email = extractAttribute(principal, List.of(
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
                "email",
                "Email",
                "mail"
        ));

        if (email == null) {
            // Fall back to NameID
            email = principal.getName();
        }

        String firstName = extractAttribute(principal, List.of(
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname",
                "firstName",
                "FirstName",
                "first_name",
                "givenName"
        ));

        String lastName = extractAttribute(principal, List.of(
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname",
                "lastName",
                "LastName",
                "last_name",
                "sn"
        ));

        log.info("SAML authentication successful for user: {} via registration: {}", email, registrationId);

        // Handle the authentication - find/create user and generate tokens
        Map<String, String> tokens = ssoService.handleSamlAuthentication(
                registrationId, email, firstName, lastName);

        // Redirect to frontend with tokens
        String redirectUrl = frontendRedirectUri
                + "?accessToken=" + tokens.get("accessToken")
                + "&refreshToken=" + tokens.get("refreshToken")
                + "&email=" + tokens.get("email")
                + "&provider=saml";

        response.sendRedirect(redirectUrl);
    }

    private String extractAttribute(Saml2AuthenticatedPrincipal principal, List<String> attributeNames) {
        for (String attrName : attributeNames) {
            List<Object> values = principal.getAttribute(attrName);
            if (values != null && !values.isEmpty()) {
                return values.get(0).toString();
            }
        }
        return null;
    }

    private String extractRegistrationId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // URI format: /login/saml2/sso/{registrationId}
        String[] parts = uri.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "unknown";
    }
}

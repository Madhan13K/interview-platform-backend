package com.interview_platform_backend.interview_platform_backend.security.oauth2;

import com.interview_platform_backend.interview_platform_backend.security.jwt.JwtService;
import com.interview_platform_backend.interview_platform_backend.security.token.RefreshTokenService;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.AuthProvider;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserProfile;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserRole;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRoleRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    public OAuth2SuccessHandler(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();

        // Reliably get the provider from the authentication token (not from URL parsing)
        String registrationId = authToken.getAuthorizedClientRegistrationId();

        // Extract user info based on provider
        String email = extractEmail(oAuth2User, registrationId);
        String firstName = extractFirstName(oAuth2User, registrationId);
        String lastName = extractLastName(oAuth2User, registrationId);

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email not found from OAuth2 provider: " + registrationId);
        }

        final String fName = firstName;
        final String lName = lastName;
        final String provider = registrationId;
        User user = userRepository.findByEmail(email).orElseGet(() -> createOAuthUser(email, fName, lName, provider));

        ensureDefaultRole(user);

        List<String> roleAuthorities = userRoleRepository.findByUser(user)
                .stream()
                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getName() != null)
                .map(userRole -> "ROLE_" + userRole.getRole().getName())
                .distinct()
                .toList();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(roleAuthorities.toArray(new String[0]))
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        refreshTokenService.create(user, refreshToken);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("email", user.getEmail())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User createOAuthUser(String email, String firstName, String lastName, String registrationId) {
        AuthProvider authProvider = switch (registrationId) {
            case "github" -> AuthProvider.GITHUB;
            case "microsoft" -> AuthProvider.MICROSOFT;
            default -> AuthProvider.GOOGLE;
        };

        User user = User.builder()
                .email(email)
                .firstName(firstName != null ? firstName : "OAuth")
                .lastName(lastName != null ? lastName : "User")
                .password("")
                .status(UserStatus.ACTIVE)
                .authProvider(authProvider)
                .createdAt(Instant.now())
                .build();

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        return userRepository.save(user);
    }

    private void ensureDefaultRole(User user) {
        boolean hasAnyRole = !userRoleRepository.findByUser(user).isEmpty();
        if (hasAnyRole) {
            return;
        }

        Role candidateRole = roleRepository.findByName("CANDIDATE")
                .orElseThrow(() -> new RuntimeException("Default role CANDIDATE not found"));

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(candidateRole)
                .assignedAt(Instant.now())
                .build();

        userRoleRepository.save(userRole);
    }


    /**
     * Extracts email based on provider.
     * - Google: "email" attribute
     * - GitHub: "email" attribute (maybe null if private; falls back to login@github)
     * - Microsoft: "email" or "preferred_username"
     */
    private String extractEmail(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId) {
            case "github" -> {
                String email = oAuth2User.getAttribute("email");
                if (email == null || email.isBlank()) {
                    // GitHub users with private emails — use login as fallback
                    String login = oAuth2User.getAttribute("login");
                    yield (login != null) ? login + "@github.oauth" : null;
                }
                yield email;
            }
            case "microsoft" -> {
                String email = oAuth2User.getAttribute("email");
                if (email == null || email.isBlank()) {
                    email = oAuth2User.getAttribute("preferred_username");
                }
                yield email;
            }
            default -> oAuth2User.getAttribute("email"); // google
        };
    }

    /**
     * Extracts first name based on provider.
     */
    private String extractFirstName(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId) {
            case "github" -> {
                String name = oAuth2User.getAttribute("name");
                if (name != null && !name.isBlank()) {
                    yield name.trim().split("\\s+", 2)[0];
                }
                yield oAuth2User.getAttribute("login");
            }
            case "microsoft" -> {
                String givenName = oAuth2User.getAttribute("givenName");
                if (givenName != null) yield givenName;
                String displayName = oAuth2User.getAttribute("displayName");
                if (displayName != null && !displayName.isBlank()) {
                    yield displayName.trim().split("\\s+", 2)[0];
                }
                yield "User";
            }
            default -> { // google
                String firstName = oAuth2User.getAttribute("given_name");
                if (firstName == null || firstName.isBlank()) {
                    String fullName = oAuth2User.getAttribute("name");
                    if (fullName != null && !fullName.isBlank()) {
                        yield fullName.trim().split("\\s+", 2)[0];
                    }
                }
                yield firstName != null ? firstName : "User";
            }
        };
    }

    /**
     * Extracts last name based on provider.
     */
    private String extractLastName(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId) {
            case "github" -> {
                String name = oAuth2User.getAttribute("name");
                if (name != null && !name.isBlank()) {
                    String[] parts = name.trim().split("\\s+", 2);
                    yield parts.length > 1 ? parts[1] : "";
                }
                yield "";
            }
            case "microsoft" -> {
                String surname = oAuth2User.getAttribute("surname");
                if (surname != null) yield surname;
                String displayName = oAuth2User.getAttribute("displayName");
                if (displayName != null && !displayName.isBlank()) {
                    String[] parts = displayName.trim().split("\\s+", 2);
                    yield parts.length > 1 ? parts[1] : "";
                }
                yield "";
            }
            default -> { // google
                String lastName = oAuth2User.getAttribute("family_name");
                if (lastName == null || lastName.isBlank()) {
                    String fullName = oAuth2User.getAttribute("name");
                    if (fullName != null && !fullName.isBlank()) {
                        String[] parts = fullName.trim().split("\\s+", 2);
                        yield parts.length > 1 ? parts[1] : "";
                    }
                }
                yield lastName != null ? lastName : "";
            }
        };
    }
}
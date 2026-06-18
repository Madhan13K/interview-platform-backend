package com.interview_platform_backend.interview_platform_backend.sso.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.jwt.JwtService;
import com.interview_platform_backend.interview_platform_backend.security.token.RefreshTokenService;
import com.interview_platform_backend.interview_platform_backend.sso.dto.SsoConfigurationRequest;
import com.interview_platform_backend.interview_platform_backend.sso.dto.SsoConfigurationResponse;
import com.interview_platform_backend.interview_platform_backend.sso.entity.SsoConfiguration;
import com.interview_platform_backend.interview_platform_backend.sso.entity.SsoProviderType;
import com.interview_platform_backend.interview_platform_backend.sso.repository.SsoConfigurationRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.*;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);

    private final SsoConfigurationRepository ssoConfigurationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${app.sso.base-url:http://localhost:8080}")
    private String baseUrl;

    public SsoService(SsoConfigurationRepository ssoConfigurationRepository,
                      UserRepository userRepository,
                      RoleRepository roleRepository,
                      UserRoleRepository userRoleRepository,
                      JwtService jwtService,
                      RefreshTokenService refreshTokenService) {
        this.ssoConfigurationRepository = ssoConfigurationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Create a new SSO/SAML configuration for a tenant.
     */
    public SsoConfigurationResponse createConfiguration(SsoConfigurationRequest request) {
        // Check for duplicates
        if (ssoConfigurationRepository.existsByTenantIdAndProviderType(
                request.getTenantId(), request.getProviderType())) {
            throw new DuplicateResourceException("SsoConfiguration", "providerType",
                    request.getProviderType().name());
        }

        // Validate certificate format
        validateCertificate(request.getIdpCertificate());

        String registrationId = generateRegistrationId(request.getTenantId(), request.getProviderType());

        SsoConfiguration config = SsoConfiguration.builder()
                .tenantId(request.getTenantId())
                .registrationId(registrationId)
                .displayName(request.getDisplayName())
                .providerType(request.getProviderType())
                .idpEntityId(request.getIdpEntityId())
                .idpSsoUrl(request.getIdpSsoUrl())
                .idpSloUrl(request.getIdpSloUrl())
                .idpCertificate(request.getIdpCertificate())
                .metadataUrl(request.getMetadataUrl())
                .spEntityId(request.getSpEntityId() != null ? request.getSpEntityId()
                        : baseUrl + "/saml2/service-provider-metadata/" + registrationId)
                .acsUrl(baseUrl + "/login/saml2/sso/" + registrationId)
                .nameIdFormat(request.getNameIdFormat() != null ? request.getNameIdFormat()
                        : "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress")
                .signRequests(request.getSignRequests() != null ? request.getSignRequests() : true)
                .autoProvisionUsers(request.getAutoProvisionUsers() != null ? request.getAutoProvisionUsers() : true)
                .defaultRole(request.getDefaultRole() != null ? request.getDefaultRole() : "CANDIDATE")
                .emailAttribute(request.getEmailAttribute())
                .firstNameAttribute(request.getFirstNameAttribute())
                .lastNameAttribute(request.getLastNameAttribute())
                .build();

        config = ssoConfigurationRepository.save(config);
        log.info("Created SSO configuration '{}' for tenant {}", config.getDisplayName(), config.getTenantId());

        return toResponse(config);
    }

    /**
     * Update an existing SSO configuration.
     */
    public SsoConfigurationResponse updateConfiguration(UUID configId, SsoConfigurationRequest request) {
        SsoConfiguration config = ssoConfigurationRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SsoConfiguration", "id", configId));

        if (request.getDisplayName() != null) config.setDisplayName(request.getDisplayName());
        if (request.getIdpEntityId() != null) config.setIdpEntityId(request.getIdpEntityId());
        if (request.getIdpSsoUrl() != null) config.setIdpSsoUrl(request.getIdpSsoUrl());
        if (request.getIdpSloUrl() != null) config.setIdpSloUrl(request.getIdpSloUrl());
        if (request.getIdpCertificate() != null) {
            validateCertificate(request.getIdpCertificate());
            config.setIdpCertificate(request.getIdpCertificate());
        }
        if (request.getMetadataUrl() != null) config.setMetadataUrl(request.getMetadataUrl());
        if (request.getSpEntityId() != null) config.setSpEntityId(request.getSpEntityId());
        if (request.getNameIdFormat() != null) config.setNameIdFormat(request.getNameIdFormat());
        if (request.getSignRequests() != null) config.setSignRequests(request.getSignRequests());
        if (request.getAutoProvisionUsers() != null) config.setAutoProvisionUsers(request.getAutoProvisionUsers());
        if (request.getDefaultRole() != null) config.setDefaultRole(request.getDefaultRole());
        if (request.getEmailAttribute() != null) config.setEmailAttribute(request.getEmailAttribute());
        if (request.getFirstNameAttribute() != null) config.setFirstNameAttribute(request.getFirstNameAttribute());
        if (request.getLastNameAttribute() != null) config.setLastNameAttribute(request.getLastNameAttribute());

        config = ssoConfigurationRepository.save(config);
        log.info("Updated SSO configuration '{}' (id: {})", config.getDisplayName(), configId);

        return toResponse(config);
    }

    /**
     * Enable/disable an SSO configuration.
     */
    public SsoConfigurationResponse toggleConfiguration(UUID configId, boolean enabled) {
        SsoConfiguration config = ssoConfigurationRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SsoConfiguration", "id", configId));

        config.setEnabled(enabled);
        config = ssoConfigurationRepository.save(config);
        log.info("SSO configuration '{}' {} (id: {})", config.getDisplayName(),
                enabled ? "enabled" : "disabled", configId);

        return toResponse(config);
    }

    /**
     * Delete an SSO configuration.
     */
    public void deleteConfiguration(UUID configId) {
        SsoConfiguration config = ssoConfigurationRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SsoConfiguration", "id", configId));
        ssoConfigurationRepository.delete(config);
        log.info("Deleted SSO configuration '{}' (id: {})", config.getDisplayName(), configId);
    }

    /**
     * Get SSO configuration by ID.
     */
    @Transactional(readOnly = true)
    public SsoConfigurationResponse getConfiguration(UUID configId) {
        SsoConfiguration config = ssoConfigurationRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SsoConfiguration", "id", configId));
        return toResponse(config);
    }

    /**
     * Get all SSO configurations for a tenant.
     */
    @Transactional(readOnly = true)
    public List<SsoConfigurationResponse> getConfigurationsForTenant(UUID tenantId) {
        return ssoConfigurationRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get all active SSO configurations (for dynamic relying party registration).
     */
    @Transactional(readOnly = true)
    public List<SsoConfiguration> getAllActiveConfigurations() {
        return ssoConfigurationRepository.findByEnabledTrue();
    }

    /**
     * Handle successful SAML authentication - find or create user and generate JWT tokens.
     */
    @Transactional
    public Map<String, String> handleSamlAuthentication(String registrationId, String email,
                                                         String firstName, String lastName) {
        SsoConfiguration config = ssoConfigurationRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SsoConfiguration", "registrationId", registrationId));

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            if (!config.getAutoProvisionUsers()) {
                throw new BadRequestException("User not found and auto-provisioning is disabled for this SSO configuration");
            }
            user = provisionUser(email, firstName, lastName, config);
        } else {
            // Update last login
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        }

        // Generate JWT tokens
        List<String> roleAuthorities = userRoleRepository.findByUser(user).stream()
                .filter(ur -> ur.getRole() != null && ur.getRole().getName() != null)
                .map(ur -> "ROLE_" + ur.getRole().getName())
                .distinct()
                .toList();

        if (roleAuthorities.isEmpty()) {
            roleAuthorities = List.of("ROLE_CANDIDATE");
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("")
                .authorities(roleAuthorities.toArray(new String[0]))
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        refreshTokenService.create(user, refreshToken);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "email", user.getEmail()
        );
    }

    private User provisionUser(String email, String firstName, String lastName, SsoConfiguration config) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName != null ? firstName : "SSO");
        user.setLastName(lastName != null ? lastName : "User");
        user.setPassword(""); // No password for SSO users
        user.setAuthProvider(AuthProvider.SAML);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setLastLoginAt(Instant.now());

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        user = userRepository.save(user);

        // Assign default role
        Role role = roleRepository.findByName(config.getDefaultRole())
                .orElseGet(() -> roleRepository.findByName("CANDIDATE")
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "CANDIDATE")));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(Instant.now());
        userRoleRepository.save(userRole);

        log.info("Auto-provisioned SAML user: {} with role: {}", email, role.getName());
        return user;
    }

    private String generateRegistrationId(UUID tenantId, SsoProviderType providerType) {
        return tenantId.toString().substring(0, 8) + "-" + providerType.name().toLowerCase().replace("_", "-");
    }

    private void validateCertificate(String certificate) {
        if (certificate == null || certificate.isBlank()) {
            throw new BadRequestException("IdP certificate cannot be empty");
        }
        // Basic validation - should contain PEM markers or be a raw base64 cert
        if (!certificate.contains("BEGIN CERTIFICATE") && !certificate.matches("[A-Za-z0-9+/=\\s]+")) {
            throw new BadRequestException("IdP certificate must be PEM-encoded or raw base64");
        }
    }

    private SsoConfigurationResponse toResponse(SsoConfiguration config) {
        return SsoConfigurationResponse.builder()
                .id(config.getId())
                .tenantId(config.getTenantId())
                .registrationId(config.getRegistrationId())
                .displayName(config.getDisplayName())
                .providerType(config.getProviderType())
                .idpEntityId(config.getIdpEntityId())
                .idpSsoUrl(config.getIdpSsoUrl())
                .idpSloUrl(config.getIdpSloUrl())
                .metadataUrl(config.getMetadataUrl())
                .spEntityId(config.getSpEntityId())
                .acsUrl(config.getAcsUrl())
                .nameIdFormat(config.getNameIdFormat())
                .signRequests(config.getSignRequests())
                .enabled(config.getEnabled())
                .autoProvisionUsers(config.getAutoProvisionUsers())
                .defaultRole(config.getDefaultRole())
                .emailAttribute(config.getEmailAttribute())
                .firstNameAttribute(config.getFirstNameAttribute())
                .lastNameAttribute(config.getLastNameAttribute())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .spMetadataUrl(baseUrl + "/saml2/service-provider-metadata/" + config.getRegistrationId())
                .loginUrl(baseUrl + "/saml2/authenticate/" + config.getRegistrationId())
                .build();
    }
}

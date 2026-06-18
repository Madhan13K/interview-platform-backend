package com.interview_platform_backend.interview_platform_backend.sso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores SAML2 Identity Provider configuration per tenant.
 * Supports Okta, OneLogin, Azure AD, and other SAML 2.0 compliant IdPs.
 */
@Entity
@Table(name = "sso_configurations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "provider_type"}),
        @UniqueConstraint(columnNames = {"registration_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Unique registration ID used in Spring Security SAML2 relying party registration.
     * Format: {tenantId}-{providerType} e.g., "acme-corp-okta"
     */
    @Column(name = "registration_id", nullable = false, unique = true, length = 100)
    private String registrationId;

    /**
     * Human-readable name for this SSO configuration.
     */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 30)
    private SsoProviderType providerType;

    /**
     * IdP Entity ID (issuer) - identifies the Identity Provider.
     */
    @Column(name = "idp_entity_id", nullable = false, length = 500)
    private String idpEntityId;

    /**
     * IdP SSO URL - where SAML AuthnRequest is sent.
     */
    @Column(name = "idp_sso_url", nullable = false, length = 500)
    private String idpSsoUrl;

    /**
     * IdP Single Logout URL (optional).
     */
    @Column(name = "idp_slo_url", length = 500)
    private String idpSloUrl;

    /**
     * IdP X.509 certificate for signature verification (PEM-encoded).
     */
    @Column(name = "idp_certificate", columnDefinition = "TEXT", nullable = false)
    private String idpCertificate;

    /**
     * IdP metadata URL for automatic configuration refresh (optional).
     * If set, the system will periodically fetch and update IdP metadata.
     */
    @Column(name = "metadata_url", length = 500)
    private String metadataUrl;

    /**
     * SP Entity ID - our Service Provider identifier.
     * Default: {baseUrl}/saml2/service-provider-metadata/{registrationId}
     */
    @Column(name = "sp_entity_id", length = 500)
    private String spEntityId;

    /**
     * Assertion Consumer Service URL.
     * Default: {baseUrl}/login/saml2/sso/{registrationId}
     */
    @Column(name = "acs_url", length = 500)
    private String acsUrl;

    /**
     * Name ID format requested from IdP.
     */
    @Column(name = "name_id_format", length = 100)
    @Builder.Default
    private String nameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";

    /**
     * Whether SAML assertions should be signed.
     */
    @Column(name = "sign_requests")
    @Builder.Default
    private Boolean signRequests = true;

    /**
     * Whether this SSO configuration is active.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Auto-provision users who authenticate via this IdP.
     */
    @Column(name = "auto_provision_users")
    @Builder.Default
    private Boolean autoProvisionUsers = true;

    /**
     * Default role to assign to auto-provisioned users.
     */
    @Column(name = "default_role", length = 50)
    @Builder.Default
    private String defaultRole = "CANDIDATE";

    /**
     * SAML attribute mapping: which IdP attribute contains the user's email.
     */
    @Column(name = "email_attribute", length = 200)
    @Builder.Default
    private String emailAttribute = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

    /**
     * SAML attribute mapping: first name.
     */
    @Column(name = "first_name_attribute", length = 200)
    @Builder.Default
    private String firstNameAttribute = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";

    /**
     * SAML attribute mapping: last name.
     */
    @Column(name = "last_name_attribute", length = 200)
    @Builder.Default
    private String lastNameAttribute = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

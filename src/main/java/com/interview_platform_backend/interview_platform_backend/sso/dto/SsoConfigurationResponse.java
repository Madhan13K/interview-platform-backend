package com.interview_platform_backend.interview_platform_backend.sso.dto;

import com.interview_platform_backend.interview_platform_backend.sso.entity.SsoProviderType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoConfigurationResponse {

    private UUID id;
    private UUID tenantId;
    private String registrationId;
    private String displayName;
    private SsoProviderType providerType;
    private String idpEntityId;
    private String idpSsoUrl;
    private String idpSloUrl;
    private String metadataUrl;
    private String spEntityId;
    private String acsUrl;
    private String nameIdFormat;
    private Boolean signRequests;
    private Boolean enabled;
    private Boolean autoProvisionUsers;
    private String defaultRole;
    private String emailAttribute;
    private String firstNameAttribute;
    private String lastNameAttribute;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * SP metadata URL for IdP configuration.
     */
    private String spMetadataUrl;

    /**
     * Login initiation URL for this SSO configuration.
     */
    private String loginUrl;
}

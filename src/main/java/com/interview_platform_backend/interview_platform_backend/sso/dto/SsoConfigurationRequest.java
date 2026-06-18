package com.interview_platform_backend.interview_platform_backend.sso.dto;

import com.interview_platform_backend.interview_platform_backend.sso.entity.SsoProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoConfigurationRequest {

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotNull(message = "Provider type is required")
    private SsoProviderType providerType;

    @NotBlank(message = "IdP Entity ID is required")
    private String idpEntityId;

    @NotBlank(message = "IdP SSO URL is required")
    private String idpSsoUrl;

    private String idpSloUrl;

    @NotBlank(message = "IdP certificate is required")
    private String idpCertificate;

    private String metadataUrl;

    private String spEntityId;

    private String nameIdFormat;

    private Boolean signRequests;

    private Boolean autoProvisionUsers;

    private String defaultRole;

    private String emailAttribute;

    private String firstNameAttribute;

    private String lastNameAttribute;
}

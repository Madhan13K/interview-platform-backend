-- V20: SSO/SAML Integration - Enterprise SSO support (Okta, OneLogin, Azure AD)
CREATE TABLE sso_configurations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    registration_id         VARCHAR(100) NOT NULL UNIQUE,
    display_name            VARCHAR(200) NOT NULL,
    provider_type           VARCHAR(30) NOT NULL,
    idp_entity_id           VARCHAR(500) NOT NULL,
    idp_sso_url             VARCHAR(500) NOT NULL,
    idp_slo_url             VARCHAR(500),
    idp_certificate         TEXT NOT NULL,
    metadata_url            VARCHAR(500),
    sp_entity_id            VARCHAR(500),
    acs_url                 VARCHAR(500),
    name_id_format          VARCHAR(100) DEFAULT 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
    sign_requests           BOOLEAN DEFAULT TRUE,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    auto_provision_users    BOOLEAN DEFAULT TRUE,
    default_role            VARCHAR(50) DEFAULT 'CANDIDATE',
    email_attribute         VARCHAR(200) DEFAULT 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress',
    first_name_attribute    VARCHAR(200) DEFAULT 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname',
    last_name_attribute     VARCHAR(200) DEFAULT 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname',
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_sso_tenant_provider UNIQUE (tenant_id, provider_type)
);

CREATE INDEX idx_sso_configurations_tenant ON sso_configurations(tenant_id);
CREATE INDEX idx_sso_configurations_enabled ON sso_configurations(enabled);
CREATE INDEX idx_sso_configurations_registration ON sso_configurations(registration_id);

-- Add SAML to auth_provider enum if column allows it (text/varchar type)
-- The AuthProvider enum in Java already includes SAML

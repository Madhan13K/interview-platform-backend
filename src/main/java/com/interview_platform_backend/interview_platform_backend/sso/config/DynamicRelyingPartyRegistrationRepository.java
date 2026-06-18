package com.interview_platform_backend.interview_platform_backend.sso.config;

import com.interview_platform_backend.interview_platform_backend.sso.entity.SsoConfiguration;
import com.interview_platform_backend.interview_platform_backend.sso.repository.SsoConfigurationRepository;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Dynamic RelyingPartyRegistrationRepository that loads SAML configurations
 * from the database. This allows runtime addition of new IdPs without restart.
 */
@Component
public class DynamicRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final SsoConfigurationRepository ssoConfigurationRepository;

    public DynamicRelyingPartyRegistrationRepository(SsoConfigurationRepository ssoConfigurationRepository) {
        this.ssoConfigurationRepository = ssoConfigurationRepository;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        return ssoConfigurationRepository.findByRegistrationId(registrationId)
                .filter(SsoConfiguration::getEnabled)
                .map(this::toRelyingPartyRegistration)
                .orElse(null);
    }

    /**
     * Find registration by asserting party entity ID (for IdP-initiated SSO).
     */
    public RelyingPartyRegistration findByAssertingPartyEntityId(String entityId) {
        return ssoConfigurationRepository.findByEnabledTrue().stream()
                .filter(config -> config.getIdpEntityId().equals(entityId))
                .findFirst()
                .map(this::toRelyingPartyRegistration)
                .orElse(null);
    }

    private RelyingPartyRegistration toRelyingPartyRegistration(SsoConfiguration config) {
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId(config.getRegistrationId());

        // Configure Asserting Party (IdP)
        builder.assertingPartyMetadata(assertingParty -> {
            assertingParty.entityId(config.getIdpEntityId());
            assertingParty.singleSignOnServiceLocation(config.getIdpSsoUrl());
            assertingParty.singleSignOnServiceBinding(Saml2MessageBinding.POST);
            assertingParty.wantAuthnRequestsSigned(config.getSignRequests());

            // Parse and set the verification certificate
            try {
                X509Certificate cert = parseCertificate(config.getIdpCertificate());
                assertingParty.verificationX509Credentials(c ->
                        c.add(new org.springframework.security.saml2.core.Saml2X509Credential(
                                cert,
                                org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType.VERIFICATION
                        ))
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse IdP certificate for registration: "
                        + config.getRegistrationId(), e);
            }

            if (config.getIdpSloUrl() != null) {
                assertingParty.singleLogoutServiceLocation(config.getIdpSloUrl());
                assertingParty.singleLogoutServiceBinding(Saml2MessageBinding.POST);
            }
        });

        // Configure Service Provider
        builder.entityId(config.getSpEntityId());
        builder.assertionConsumerServiceLocation(config.getAcsUrl());

        return builder.build();
    }

    private X509Certificate parseCertificate(String certString) throws Exception {
        // Strip PEM headers if present
        String cleanCert = certString
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleanCert);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
    }
}

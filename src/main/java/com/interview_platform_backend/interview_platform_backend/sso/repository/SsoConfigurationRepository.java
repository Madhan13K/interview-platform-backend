package com.interview_platform_backend.interview_platform_backend.sso.repository;

import com.interview_platform_backend.interview_platform_backend.sso.entity.SsoConfiguration;
import com.interview_platform_backend.interview_platform_backend.sso.entity.SsoProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SsoConfigurationRepository extends JpaRepository<SsoConfiguration, UUID> {

    List<SsoConfiguration> findByTenantId(UUID tenantId);

    Optional<SsoConfiguration> findByRegistrationId(String registrationId);

    Optional<SsoConfiguration> findByTenantIdAndProviderType(UUID tenantId, SsoProviderType providerType);

    List<SsoConfiguration> findByEnabledTrue();

    boolean existsByTenantIdAndProviderType(UUID tenantId, SsoProviderType providerType);
}

package com.interview_platform_backend.interview_platform_backend.webhook.repository;

import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    List<WebhookEndpoint> findByUserId(UUID userId);

    List<WebhookEndpoint> findByIsActiveTrue();

    List<WebhookEndpoint> findByOrganizationId(UUID organizationId);
}

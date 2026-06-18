package com.interview_platform_backend.interview_platform_backend.webhook.repository;

import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findByEndpointId(UUID endpointId, Pageable pageable);

    List<WebhookDelivery> findByStatus(WebhookDelivery.DeliveryStatus status);

    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(WebhookDelivery.DeliveryStatus status, Instant before);
}

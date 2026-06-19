package com.interview_platform_backend.interview_platform_backend.webhook.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.webhook.dto.*;
import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookDelivery;
import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookEndpoint;
import com.interview_platform_backend.interview_platform_backend.webhook.repository.WebhookDeliveryRepository;
import com.interview_platform_backend.interview_platform_backend.webhook.repository.WebhookEndpointRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class WebhookService {

    private static final Set<String> VALID_EVENTS = Set.of(
            "INTERVIEW_SCHEDULED",
            "INTERVIEW_COMPLETED",
            "INTERVIEW_CANCELLED",
            "FEEDBACK_SUBMITTED",
            "CANDIDATE_HIRED",
            "CANDIDATE_REJECTED"
    );

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final UserRepository userRepository;

    public WebhookService(WebhookEndpointRepository webhookEndpointRepository,
                          WebhookDeliveryRepository webhookDeliveryRepository,
                          UserRepository userRepository) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.userRepository = userRepository;
    }

    @CacheEvict(value = "webhooks", allEntries = true)
    public WebhookEndpointResponse createWebhook(CreateWebhookRequest request, UUID userId) {
        validateEvents(request.getEvents());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String secret = "whsec_" + UUID.randomUUID().toString().replace("-", "");

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .organizationId(userId)
                .user(user)
                .url(request.getUrl())
                .secret(secret)
                .description(request.getDescription())
                .events(request.getEvents().toArray(new String[0]))
                .isActive(true)
                .build();

        WebhookEndpoint saved = webhookEndpointRepository.save(endpoint);
        return mapToResponse(saved, false);
    }

    @CacheEvict(value = "webhooks", allEntries = true)
    public WebhookEndpointResponse updateWebhook(UUID id, UpdateWebhookRequest request) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", "id", id));

        if (request.getUrl() != null) {
            endpoint.setUrl(request.getUrl());
        }
        if (request.getDescription() != null) {
            endpoint.setDescription(request.getDescription());
        }
        if (request.getEvents() != null) {
            validateEvents(request.getEvents());
            endpoint.setEvents(request.getEvents().toArray(new String[0]));
        }
        if (request.getIsActive() != null) {
            endpoint.setIsActive(request.getIsActive());
        }

        WebhookEndpoint saved = webhookEndpointRepository.save(endpoint);
        return mapToResponse(saved, true);
    }

    @CacheEvict(value = "webhooks", allEntries = true)
    public void deleteWebhook(UUID id) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", "id", id));
        webhookEndpointRepository.delete(endpoint);
    }

    @Transactional(readOnly = true)
    public WebhookEndpointResponse getWebhook(UUID id) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", "id", id));
        return mapToResponse(endpoint, true);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> getMyWebhooks(UUID userId) {
        return webhookEndpointRepository.findByUserId(userId).stream()
                .map(endpoint -> mapToResponse(endpoint, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<WebhookDeliveryResponse> getDeliveries(UUID endpointId, int page, int size) {
        webhookEndpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", "id", endpointId));

        Page<WebhookDelivery> deliveryPage = webhookDeliveryRepository.findByEndpointId(
                endpointId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<WebhookDeliveryResponse> content = deliveryPage.getContent().stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.<WebhookDeliveryResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(deliveryPage.getTotalElements())
                .totalPages(deliveryPage.getTotalPages())
                .last(deliveryPage.isLast())
                .build();
    }

    public WebhookDeliveryResponse retryDelivery(UUID deliveryId) {
        WebhookDelivery delivery = webhookDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery", "id", deliveryId));

        if (delivery.getStatus() != WebhookDelivery.DeliveryStatus.FAILED &&
                delivery.getStatus() != WebhookDelivery.DeliveryStatus.RETRYING) {
            throw new BadRequestException("Only FAILED or RETRYING deliveries can be retried");
        }

        // Persist retry state to DB so it survives app restarts
        delivery.setStatus(WebhookDelivery.DeliveryStatus.PENDING);
        delivery.setAttempt(0);
        delivery.setNextRetryAt(null);
        delivery.setResponseBody(null);
        delivery.setResponseStatus(null);

        WebhookDelivery saved = webhookDeliveryRepository.save(delivery);
        return mapToDeliveryResponse(saved);
    }

    @CacheEvict(value = "webhooks", allEntries = true)
    public WebhookEndpointResponse regenerateSecret(UUID id) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEndpoint", "id", id));

        String newSecret = "whsec_" + UUID.randomUUID().toString().replace("-", "");
        endpoint.setSecret(newSecret);

        WebhookEndpoint saved = webhookEndpointRepository.save(endpoint);
        return mapToResponse(saved, false);
    }

    private void validateEvents(List<String> events) {
        for (String event : events) {
            if (!VALID_EVENTS.contains(event)) {
                throw new BadRequestException("Invalid event type: " + event +
                        ". Valid events are: " + String.join(", ", VALID_EVENTS));
            }
        }
    }

    private WebhookEndpointResponse mapToResponse(WebhookEndpoint endpoint, boolean maskSecret) {
        String secret = endpoint.getSecret();
        if (maskSecret && secret != null && secret.length() > 4) {
            secret = "****" + secret.substring(secret.length() - 4);
        }

        return WebhookEndpointResponse.builder()
                .id(endpoint.getId())
                .url(endpoint.getUrl())
                .description(endpoint.getDescription())
                .events(endpoint.getEvents() != null ? Arrays.asList(endpoint.getEvents()) : List.of())
                .isActive(endpoint.getIsActive())
                .secret(secret)
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }

    private WebhookDeliveryResponse mapToDeliveryResponse(WebhookDelivery delivery) {
        return WebhookDeliveryResponse.builder()
                .id(delivery.getId())
                .endpointId(delivery.getEndpoint().getId())
                .eventType(delivery.getEventType())
                .payload(delivery.getPayload())
                .responseStatus(delivery.getResponseStatus())
                .responseBody(delivery.getResponseBody())
                .attempt(delivery.getAttempt())
                .maxAttempts(delivery.getMaxAttempts())
                .status(delivery.getStatus())
                .nextRetryAt(delivery.getNextRetryAt())
                .deliveredAt(delivery.getDeliveredAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}

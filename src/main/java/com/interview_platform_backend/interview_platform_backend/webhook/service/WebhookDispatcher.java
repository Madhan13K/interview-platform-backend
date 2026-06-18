package com.interview_platform_backend.interview_platform_backend.webhook.service;

import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookDelivery;
import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookEndpoint;
import com.interview_platform_backend.interview_platform_backend.webhook.repository.WebhookDeliveryRepository;
import com.interview_platform_backend.interview_platform_backend.webhook.repository.WebhookEndpointRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    private static final int MAX_ATTEMPTS = 5;

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final RestTemplate restTemplate;

    public WebhookDispatcher(WebhookEndpointRepository webhookEndpointRepository,
                             WebhookDeliveryRepository webhookDeliveryRepository) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void dispatchEvent(String eventType, String payload) {
        List<WebhookEndpoint> activeEndpoints = webhookEndpointRepository.findByIsActiveTrue();

        for (WebhookEndpoint endpoint : activeEndpoints) {
            if (endpoint.getEvents() != null &&
                    Arrays.asList(endpoint.getEvents()).contains(eventType)) {

                WebhookDelivery delivery = WebhookDelivery.builder()
                        .endpoint(endpoint)
                        .eventType(eventType)
                        .payload(payload)
                        .attempt(0)
                        .maxAttempts(MAX_ATTEMPTS)
                        .status(WebhookDelivery.DeliveryStatus.PENDING)
                        .build();

                WebhookDelivery saved = webhookDeliveryRepository.save(delivery);
                deliverWebhookAsync(saved.getId());
            }
        }
    }

    @Async
    public void deliverWebhookAsync(UUID deliveryId) {
        webhookDeliveryRepository.findById(deliveryId).ifPresent(this::deliverWebhook);
    }

    @Transactional
    @CircuitBreaker(name = "webhookService", fallbackMethod = "deliverWebhookFallback")
    public void deliverWebhook(WebhookDelivery delivery) {
        WebhookEndpoint endpoint = delivery.getEndpoint();

        try {
            String signature = computeHmacSha256(delivery.getPayload(), endpoint.getSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Webhook-Event", delivery.getEventType());
            headers.set("X-Webhook-Delivery-Id", delivery.getId().toString());

            HttpEntity<String> requestEntity = new HttpEntity<>(delivery.getPayload(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            delivery.setResponseStatus(response.getStatusCode().value());
            delivery.setResponseBody(response.getBody());
            delivery.setAttempt(delivery.getAttempt() + 1);
            delivery.setStatus(WebhookDelivery.DeliveryStatus.DELIVERED);
            delivery.setDeliveredAt(Instant.now());

        } catch (Exception e) {
            log.error("Webhook delivery failed for delivery {}: {}", delivery.getId(), e.getMessage());

            delivery.setAttempt(delivery.getAttempt() + 1);
            delivery.setResponseBody(e.getMessage());

            if (delivery.getAttempt() >= delivery.getMaxAttempts()) {
                delivery.setStatus(WebhookDelivery.DeliveryStatus.FAILED);
            } else {
                delivery.setStatus(WebhookDelivery.DeliveryStatus.RETRYING);
                delivery.setNextRetryAt(calculateNextRetry(delivery.getAttempt()));
            }
        }

        webhookDeliveryRepository.save(delivery);
    }

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "webhookProcessRetries", lockAtLeastFor = "1m")
    @Transactional
    public void processRetries() {
        List<WebhookDelivery> retryable = webhookDeliveryRepository
                .findByStatusAndNextRetryAtBefore(
                        WebhookDelivery.DeliveryStatus.RETRYING,
                        Instant.now()
                );

        for (WebhookDelivery delivery : retryable) {
            deliverWebhook(delivery);
        }
    }

    private Instant calculateNextRetry(int attempt) {
        // Exponential backoff: 15s, 60s, 240s, 960s, ...
        long delaySeconds = (long) (15 * Math.pow(4, attempt - 1));
        return Instant.now().plusSeconds(delaySeconds);
    }

    private String computeHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

    private void deliverWebhookFallback(WebhookDelivery delivery, Throwable throwable) {
        log.warn("Circuit breaker open for webhook delivery {}. Marking as RETRYING. Cause: {}",
                delivery.getId(), throwable.getMessage());
        delivery.setAttempt(delivery.getAttempt() + 1);
        delivery.setResponseBody("Circuit breaker open: " + throwable.getMessage());
        if (delivery.getAttempt() >= delivery.getMaxAttempts()) {
            delivery.setStatus(WebhookDelivery.DeliveryStatus.FAILED);
        } else {
            delivery.setStatus(WebhookDelivery.DeliveryStatus.RETRYING);
            delivery.setNextRetryAt(calculateNextRetry(delivery.getAttempt()));
        }
        webhookDeliveryRepository.save(delivery);
    }
}

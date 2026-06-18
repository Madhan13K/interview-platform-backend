package com.interview_platform_backend.interview_platform_backend.webhook.dto;

import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookDelivery;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryResponse {

    private UUID id;

    private UUID endpointId;

    private String eventType;

    private String payload;

    private Integer responseStatus;

    private String responseBody;

    private Integer attempt;

    private Integer maxAttempts;

    private WebhookDelivery.DeliveryStatus status;

    private Instant nextRetryAt;

    private Instant deliveredAt;

    private Instant createdAt;
}

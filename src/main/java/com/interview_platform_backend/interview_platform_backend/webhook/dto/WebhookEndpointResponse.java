package com.interview_platform_backend.interview_platform_backend.webhook.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEndpointResponse {

    private UUID id;

    private String url;

    private String description;

    private List<String> events;

    private Boolean isActive;

    private String secret;

    private Instant createdAt;

    private Instant updatedAt;
}

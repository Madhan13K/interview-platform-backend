package com.interview_platform_backend.interview_platform_backend.webhook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String payload;

    private Integer responseStatus;

    @Column(columnDefinition = "text")
    private String responseBody;

    @Builder.Default
    @Column(nullable = false)
    private Integer attempt = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxAttempts = 5;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    private Instant nextRetryAt;

    private Instant deliveredAt;

    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum DeliveryStatus {
        PENDING,
        DELIVERED,
        FAILED,
        RETRYING
    }
}

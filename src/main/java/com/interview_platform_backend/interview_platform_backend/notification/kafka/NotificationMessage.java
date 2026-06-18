package com.interview_platform_backend.interview_platform_backend.notification.kafka;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unified notification message published to Kafka.
 * Consumers decide which channel(s) to deliver on (EMAIL, SMS, PUSH).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    private String eventType;           // INTERVIEW_SCHEDULED, INTERVIEW_CANCELLED, etc.
    private List<String> channels;      // EMAIL, SMS, PUSH
    private String recipientEmail;
    private String recipientPhone;
    private String recipientName;
    private String subject;
    private String body;
    private Map<String, String> metadata; // extra data (interviewId, link, etc.)
    private Instant timestamp;
}


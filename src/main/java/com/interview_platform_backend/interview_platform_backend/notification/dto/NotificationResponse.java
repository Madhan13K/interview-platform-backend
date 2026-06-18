package com.interview_platform_backend.interview_platform_backend.notification.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String type;
    private String title;
    private String message;
    private UUID referenceId;
    private String referenceType;
    private Boolean isRead;
    private Instant createdAt;
    private Instant readAt;
}


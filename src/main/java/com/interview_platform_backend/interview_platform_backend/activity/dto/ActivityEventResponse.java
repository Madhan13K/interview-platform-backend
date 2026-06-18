package com.interview_platform_backend.interview_platform_backend.activity.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEventResponse {

    private UUID id;
    private UUID actorId;
    private String actorName;
    private String actorEmail;
    private String action;
    private String entityType;
    private UUID entityId;
    private String targetType;
    private UUID targetId;
    private Map<String, Object> metadata;
    private Instant createdAt;
}

package com.interview_platform_backend.interview_platform_backend.activity.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFilterRequest {

    private String entityType;
    private UUID entityId;
    private UUID actorId;
    private String action;
    private Instant startDate;
    private Instant endDate;
}

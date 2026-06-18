package com.interview_platform_backend.interview_platform_backend.gdpr.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErasureRequestResponse {

    private UUID id;
    private UUID userId;
    private String status;
    private Instant requestedAt;
    private Instant completedAt;
}

package com.interview_platform_backend.interview_platform_backend.gdpr.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentResponse {

    private UUID id;
    private String consentType;
    private Boolean granted;
    private Instant grantedAt;
    private Instant revokedAt;
}

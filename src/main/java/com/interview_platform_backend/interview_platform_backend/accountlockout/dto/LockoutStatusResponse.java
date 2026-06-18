package com.interview_platform_backend.interview_platform_backend.accountlockout.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockoutStatusResponse {
    private String email;
    private Integer failedAttempts;
    private Boolean locked;
    private Instant lockedAt;
    private Instant lockExpiresAt;
    private Instant lastFailedAt;
}

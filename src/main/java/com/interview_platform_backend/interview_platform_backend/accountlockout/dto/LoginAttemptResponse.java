package com.interview_platform_backend.interview_platform_backend.accountlockout.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttemptResponse {
    private UUID id;
    private String email;
    private String ipAddress;
    private String userAgent;
    private boolean successful;
    private String failureReason;
    private Instant attemptedAt;
}

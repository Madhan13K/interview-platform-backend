package com.interview_platform_backend.interview_platform_backend.calendarsync.provider;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {
}

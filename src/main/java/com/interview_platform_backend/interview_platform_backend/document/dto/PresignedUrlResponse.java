package com.interview_platform_backend.interview_platform_backend.document.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlResponse {
    private String uploadUrl;
    private String downloadUrl;
    private long expiresInSeconds;
}


package com.interview_platform_backend.interview_platform_backend.security.apikey.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiKeyResponse {

    private UUID id;
    private String name;
    private String keyPrefix;
    private List<String> scopes;
    private Boolean isActive;
    private Instant lastUsedAt;
    private Instant expiresAt;
    private Instant createdAt;

    /**
     * Only returned on creation - the full API key value.
     * This is the only time the full key is visible.
     */
    private String fullKey;
}

package com.interview_platform_backend.interview_platform_backend.security.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyRequest {

    @NotBlank(message = "API key name is required")
    private String name;

    @NotEmpty(message = "At least one scope is required")
    private List<String> scopes;

    private Instant expiresAt;
}

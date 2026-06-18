package com.interview_platform_backend.interview_platform_backend.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWebhookRequest {

    @NotBlank(message = "URL is required")
    private String url;

    private String description;

    @NotEmpty(message = "At least one event must be specified")
    private List<String> events;
}

package com.interview_platform_backend.interview_platform_backend.webhook.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWebhookRequest {

    private String url;

    private String description;

    private List<String> events;

    private Boolean isActive;
}

package com.interview_platform_backend.interview_platform_backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    @NotBlank(message = "Organization slug is required")
    private String slug;

    private String domain;

    private String logoUrl;

    private String plan;
}

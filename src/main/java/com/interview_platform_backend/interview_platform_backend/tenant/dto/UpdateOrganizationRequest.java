package com.interview_platform_backend.interview_platform_backend.tenant.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizationRequest {

    private String name;

    private String slug;

    private String domain;

    private String logoUrl;

    private String plan;

    private Integer maxUsers;

    private Boolean isActive;
}

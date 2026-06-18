package com.interview_platform_backend.interview_platform_backend.tenant.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {

    private UUID id;

    private String name;

    private String slug;

    private String domain;

    private String logoUrl;

    private String plan;

    private Integer maxUsers;

    private Boolean isActive;

    private Long memberCount;

    private Instant createdAt;

    private Instant updatedAt;
}

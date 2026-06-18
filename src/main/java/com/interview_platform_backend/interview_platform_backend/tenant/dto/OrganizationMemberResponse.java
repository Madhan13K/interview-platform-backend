package com.interview_platform_backend.interview_platform_backend.tenant.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMemberResponse {

    private UUID id;

    private UUID userId;

    private String userEmail;

    private String userName;

    private String role;

    private Instant joinedAt;
}

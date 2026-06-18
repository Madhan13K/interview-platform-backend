package com.interview_platform_backend.interview_platform_backend.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private UUID id;

    private String name;

    private String description;

    private Instant createdAt;
}
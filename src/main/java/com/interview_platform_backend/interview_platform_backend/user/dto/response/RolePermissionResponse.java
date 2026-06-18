package com.interview_platform_backend.interview_platform_backend.user.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionResponse {

    private UUID id;

    private UUID roleId;

    private String roleName;

    private UUID permissionId;

    private String permissionName;

    private Instant createdAt;
}
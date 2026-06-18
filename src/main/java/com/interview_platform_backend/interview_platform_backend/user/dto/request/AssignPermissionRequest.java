package com.interview_platform_backend.interview_platform_backend.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignPermissionRequest {

    @NotNull
    private UUID permissionId;
}
package com.interview_platform_backend.interview_platform_backend.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRoleRequest {

    @NotNull(message = "Role ID is required")
    private UUID roleId;

}
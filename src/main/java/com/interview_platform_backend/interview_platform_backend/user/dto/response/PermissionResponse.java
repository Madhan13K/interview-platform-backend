package com.interview_platform_backend.interview_platform_backend.user.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private UUID id;

    private String name;

    private String description;
}
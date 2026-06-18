package com.interview_platform_backend.interview_platform_backend.user.mapper;

import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionResponse toResponse(Permission permission) {

        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}


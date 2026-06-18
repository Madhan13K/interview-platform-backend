package com.interview_platform_backend.interview_platform_backend.user.mapper;

import com.interview_platform_backend.interview_platform_backend.user.dto.response.RolePermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.RolePermission;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionMapper {

    public RolePermissionResponse toResponse(RolePermission rolePermission) {

        return RolePermissionResponse.builder()
                .id(rolePermission.getId())
                .roleId(rolePermission.getRole().getId())
                .roleName(rolePermission.getRole().getName())
                .permissionId(rolePermission.getPermission().getId())
                .permissionName(rolePermission.getPermission().getName())
                .createdAt(rolePermission.getCreatedAt())
                .build();
    }
}


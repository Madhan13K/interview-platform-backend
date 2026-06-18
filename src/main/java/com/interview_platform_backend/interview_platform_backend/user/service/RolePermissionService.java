package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.user.dto.response.RolePermissionResponse;

import java.util.List;
import java.util.UUID;

public interface RolePermissionService {

    RolePermissionResponse assignPermissionToRole(
            UUID roleId,
            UUID permissionId
    );

    List<RolePermissionResponse> getPermissionsByRole(
            UUID roleId
    );

    void removePermissionFromRole(
            UUID rolePermissionId
    );
}
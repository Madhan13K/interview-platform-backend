package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreatePermissionRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;

import java.util.List;
import java.util.UUID;

public interface PermissionService {

    PermissionResponse createPermission(
            CreatePermissionRequest request
    );

    List<PermissionResponse> getAllPermissions();

    PermissionResponse getPermissionById(UUID permissionId);

    PermissionResponse updatePermission(
            UUID permissionId,
            CreatePermissionRequest request
    );

    void deletePermission(UUID permissionId);
}
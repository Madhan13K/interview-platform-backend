package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreateRoleRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    RoleResponse createRole(CreateRoleRequest request);

    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(UUID roleId);

    RoleResponse updateRole(UUID roleId, CreateRoleRequest request);

    void deleteRole(UUID roleId);
}
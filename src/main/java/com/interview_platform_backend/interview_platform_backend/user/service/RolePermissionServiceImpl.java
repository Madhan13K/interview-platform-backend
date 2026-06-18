package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RolePermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Permission;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.RolePermission;
import com.interview_platform_backend.interview_platform_backend.user.mapper.RolePermissionMapper;
import com.interview_platform_backend.interview_platform_backend.user.repository.PermissionRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.RolePermissionRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionMapper rolePermissionMapper;

    public RolePermissionServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository,
                                     RolePermissionRepository rolePermissionRepository,
                                     RolePermissionMapper rolePermissionMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public RolePermissionResponse assignPermissionToRole(UUID roleId, UUID permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));

        boolean exists = rolePermissionRepository.existsByRoleAndPermission(role, permission);
        if (exists) {
            throw new DuplicateResourceException("Permission already assigned to role");
        }

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermission.setCreatedAt(Instant.now());

        RolePermission savedRolePermission = rolePermissionRepository.save(rolePermission);
        return rolePermissionMapper.toResponse(savedRolePermission);
    }

    @Override
    public List<RolePermissionResponse> getPermissionsByRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        return rolePermissionRepository.findByRole(role)
                .stream()
                .map(rolePermissionMapper::toResponse)
                .toList();
    }

    @Override
    public void removePermissionFromRole(UUID rolePermissionId) {
        RolePermission rolePermission = rolePermissionRepository.findById(rolePermissionId)
                .orElseThrow(() -> new ResourceNotFoundException("RolePermission", "id", rolePermissionId));
        rolePermissionRepository.delete(rolePermission);
    }

}
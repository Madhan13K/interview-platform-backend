package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreatePermissionRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Permission;
import com.interview_platform_backend.interview_platform_backend.user.mapper.PermissionMapper;
import com.interview_platform_backend.interview_platform_backend.user.repository.PermissionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionRepository permissionRepository, PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Permission", "name", request.getName());
        }

        Permission permission = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponse(savedPermission);
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "permissions", key = "#permissionId")
    public PermissionResponse getPermissionById(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
        return permissionMapper.toResponse(permission);
    }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionResponse updatePermission(UUID permissionId, CreatePermissionRequest request) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));

        if (request.getName() != null
                && !request.getName().equalsIgnoreCase(permission.getName())
                && permissionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Permission", "name", request.getName());
        }

        if (request.getName() != null) {
            permission.setName(request.getName());
        }
        permission.setDescription(request.getDescription());

        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponse(savedPermission);
    }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public void deletePermission(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
        permissionRepository.delete(permission);
    }
}

package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreateRoleRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.mapper.RoleMapper;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    @CacheEvict(value = "roles", allEntries = true)
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Role", "name", request.getName());
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(Instant.now())
                .build();

        Role savedRole = roleRepository.save(role);
        return roleMapper.toResponse(savedRole);
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "roles", key = "#roleId")
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        return roleMapper.toResponse(role);
    }

    @Override
    @CacheEvict(value = "roles", allEntries = true)
    public RoleResponse updateRole(UUID roleId, CreateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (request.getName() != null
                && !request.getName().equalsIgnoreCase(role.getName())
                && roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Role", "name", request.getName());
        }

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        role.setDescription(request.getDescription());

        Role savedRole = roleRepository.save(role);
        return roleMapper.toResponse(savedRole);
    }

    @Override
    @CacheEvict(value = "roles", allEntries = true)
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        roleRepository.delete(role);
    }

}
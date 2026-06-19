package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RolePermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Permission;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.repository.PermissionRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class RolePermissionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testRole = roleRepository.save(Role.builder()
                .name("RP_TEST_ROLE_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .description("Role for role-permission tests")
                .createdAt(Instant.now())
                .build());

        testPermission = permissionRepository.save(Permission.builder()
                .name("RP_TEST_PERM_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .description("Permission for role-permission tests")
                .build());
    }

    @Nested
    @DisplayName("Assign Permission to Role")
    class AssignPermission {

        @Test
        @DisplayName("should assign permission to role successfully")
        void assignPermission_success() {
            RolePermissionResponse response = rolePermissionService
                    .assignPermissionToRole(testRole.getId(), testPermission.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when permission already assigned")
        void assignPermission_duplicate() {
            rolePermissionService.assignPermissionToRole(testRole.getId(), testPermission.getId());

            assertThatThrownBy(() ->
                    rolePermissionService.assignPermissionToRole(testRole.getId(), testPermission.getId()))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid role ID")
        void assignPermission_roleNotFound() {
            assertThatThrownBy(() ->
                    rolePermissionService.assignPermissionToRole(UUID.randomUUID(), testPermission.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid permission ID")
        void assignPermission_permissionNotFound() {
            assertThatThrownBy(() ->
                    rolePermissionService.assignPermissionToRole(testRole.getId(), UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Permissions by Role")
    class GetPermissionsByRole {

        @Test
        @DisplayName("should return permissions for role")
        void getPermissionsByRole_success() {
            rolePermissionService.assignPermissionToRole(testRole.getId(), testPermission.getId());

            List<RolePermissionResponse> permissions = rolePermissionService
                    .getPermissionsByRole(testRole.getId());

            assertThat(permissions).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list for role with no permissions")
        void getPermissionsByRole_empty() {
            List<RolePermissionResponse> permissions = rolePermissionService
                    .getPermissionsByRole(testRole.getId());

            assertThat(permissions).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid role ID")
        void getPermissionsByRole_roleNotFound() {
            assertThatThrownBy(() ->
                    rolePermissionService.getPermissionsByRole(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Remove Permission from Role")
    class RemovePermission {

        @Test
        @DisplayName("should remove permission from role successfully")
        void removePermission_success() {
            RolePermissionResponse assigned = rolePermissionService
                    .assignPermissionToRole(testRole.getId(), testPermission.getId());

            rolePermissionService.removePermissionFromRole(assigned.getId());

            List<RolePermissionResponse> permissions = rolePermissionService
                    .getPermissionsByRole(testRole.getId());
            assertThat(permissions).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid role-permission ID")
        void removePermission_notFound() {
            assertThatThrownBy(() ->
                    rolePermissionService.removePermissionFromRole(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}


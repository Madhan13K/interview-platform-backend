package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreateRoleRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class RoleServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RoleService roleService;

    private String roleName;

    @BeforeEach
    void setUp() {
        roleName = "ROLE_TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CreateRoleRequest buildCreateRoleRequest() {
        return CreateRoleRequest.builder()
                .name(roleName)
                .description("Test role description")
                .build();
    }

    @Nested
    @DisplayName("Create Role")
    class CreateRole {

        @Test
        @DisplayName("should create role successfully")
        void createRole_success() {
            RoleResponse response = roleService.createRole(buildCreateRoleRequest());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getName()).isEqualTo(roleName);
            assertThat(response.getDescription()).isEqualTo("Test role description");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate role name")
        void createRole_duplicate() {
            roleService.createRole(buildCreateRoleRequest());

            assertThatThrownBy(() -> roleService.createRole(buildCreateRoleRequest()))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("Get Roles")
    class GetRoles {

        @Test
        @DisplayName("should return all roles")
        void getAllRoles_success() {
            roleService.createRole(buildCreateRoleRequest());
            List<RoleResponse> roles = roleService.getAllRoles();

            assertThat(roles).isNotEmpty();
            assertThat(roles).anyMatch(r -> r.getName().equals(roleName));
        }

        @Test
        @DisplayName("should get role by ID")
        void getRoleById_success() {
            RoleResponse created = roleService.createRole(buildCreateRoleRequest());
            RoleResponse found = roleService.getRoleById(created.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getName()).isEqualTo(roleName);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid role ID")
        void getRoleById_notFound() {
            assertThatThrownBy(() -> roleService.getRoleById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Role")
    class UpdateRole {

        @Test
        @DisplayName("should update role name and description")
        void updateRole_success() {
            RoleResponse created = roleService.createRole(buildCreateRoleRequest());

            String updatedName = "UPDATED_" + roleName;
            CreateRoleRequest updateRequest = CreateRoleRequest.builder()
                    .name(updatedName)
                    .description("Updated description")
                    .build();

            RoleResponse updated = roleService.updateRole(created.getId(), updateRequest);

            assertThat(updated.getName()).isEqualTo(updatedName);
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when updating to existing name")
        void updateRole_duplicateName() {
            RoleResponse first = roleService.createRole(buildCreateRoleRequest());

            String secondName = "SECOND_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            RoleResponse second = roleService.createRole(CreateRoleRequest.builder()
                    .name(secondName)
                    .description("Second role")
                    .build());

            CreateRoleRequest updateToFirstName = CreateRoleRequest.builder()
                    .name(roleName)
                    .description("Trying to duplicate")
                    .build();

            assertThatThrownBy(() -> roleService.updateRole(second.getId(), updateToFirstName))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid role ID")
        void updateRole_notFound() {
            CreateRoleRequest request = buildCreateRoleRequest();
            assertThatThrownBy(() -> roleService.updateRole(UUID.randomUUID(), request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Role")
    class DeleteRole {

        @Test
        @DisplayName("should delete role successfully")
        void deleteRole_success() {
            RoleResponse created = roleService.createRole(buildCreateRoleRequest());
            roleService.deleteRole(created.getId());

            assertThatThrownBy(() -> roleService.getRoleById(created.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid role ID")
        void deleteRole_notFound() {
            assertThatThrownBy(() -> roleService.deleteRole(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}


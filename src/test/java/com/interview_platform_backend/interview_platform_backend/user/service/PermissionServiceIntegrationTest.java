package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreatePermissionRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;
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
class PermissionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PermissionService permissionService;

    private String permissionName;

    @BeforeEach
    void setUp() {
        permissionName = "PERM_TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CreatePermissionRequest buildCreatePermissionRequest() {
        return CreatePermissionRequest.builder()
                .name(permissionName)
                .description("Test permission description")
                .build();
    }

    @Nested
    @DisplayName("Create Permission")
    class CreatePermission {

        @Test
        @DisplayName("should create permission successfully")
        void createPermission_success() {
            PermissionResponse response = permissionService.createPermission(buildCreatePermissionRequest());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getName()).isEqualTo(permissionName);
            assertThat(response.getDescription()).isEqualTo("Test permission description");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate name")
        void createPermission_duplicate() {
            permissionService.createPermission(buildCreatePermissionRequest());

            assertThatThrownBy(() -> permissionService.createPermission(buildCreatePermissionRequest()))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("Get Permissions")
    class GetPermissions {

        @Test
        @DisplayName("should return all permissions")
        void getAllPermissions_success() {
            permissionService.createPermission(buildCreatePermissionRequest());
            List<PermissionResponse> permissions = permissionService.getAllPermissions();

            assertThat(permissions).isNotEmpty();
            assertThat(permissions).anyMatch(p -> p.getName().equals(permissionName));
        }

        @Test
        @DisplayName("should get permission by ID")
        void getPermissionById_success() {
            PermissionResponse created = permissionService.createPermission(buildCreatePermissionRequest());
            PermissionResponse found = permissionService.getPermissionById(created.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getName()).isEqualTo(permissionName);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void getPermissionById_notFound() {
            assertThatThrownBy(() -> permissionService.getPermissionById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Permission")
    class UpdatePermission {

        @Test
        @DisplayName("should update permission name and description")
        void updatePermission_success() {
            PermissionResponse created = permissionService.createPermission(buildCreatePermissionRequest());

            String updatedName = "UPDATED_" + permissionName;
            CreatePermissionRequest updateRequest = CreatePermissionRequest.builder()
                    .name(updatedName)
                    .description("Updated description")
                    .build();

            PermissionResponse updated = permissionService.updatePermission(created.getId(), updateRequest);

            assertThat(updated.getName()).isEqualTo(updatedName);
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when updating to existing name")
        void updatePermission_duplicateName() {
            PermissionResponse first = permissionService.createPermission(buildCreatePermissionRequest());

            String secondName = "SECOND_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            PermissionResponse second = permissionService.createPermission(CreatePermissionRequest.builder()
                    .name(secondName)
                    .description("Second permission")
                    .build());

            CreatePermissionRequest updateToFirstName = CreatePermissionRequest.builder()
                    .name(permissionName)
                    .description("Trying to duplicate")
                    .build();

            assertThatThrownBy(() -> permissionService.updatePermission(second.getId(), updateToFirstName))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void updatePermission_notFound() {
            CreatePermissionRequest request = buildCreatePermissionRequest();
            assertThatThrownBy(() -> permissionService.updatePermission(UUID.randomUUID(), request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Permission")
    class DeletePermission {

        @Test
        @DisplayName("should delete permission successfully")
        void deletePermission_success() {
            PermissionResponse created = permissionService.createPermission(buildCreatePermissionRequest());
            permissionService.deletePermission(created.getId());

            assertThatThrownBy(() -> permissionService.getPermissionById(created.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void deletePermission_notFound() {
            assertThatThrownBy(() -> permissionService.deletePermission(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}


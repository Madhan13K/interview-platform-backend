package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.ChangePasswordRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreateUserRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UpdateUserRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UserSearchRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "user-test-" + UUID.randomUUID() + "@example.com";
    }

    private CreateUserRequest buildCreateUserRequest() {
        return CreateUserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(testEmail)
                .password("Password@123")
                .phoneNumber("1234567890")
                .build();
    }

    @Nested
    @DisplayName("Create User")
    class CreateUser {

        @Test
        @DisplayName("should create user successfully")
        void createUser_success() {
            UserResponse response = userService.createUser(buildCreateUserRequest());

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(testEmail);
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate email")
        void createUser_duplicateEmail() {
            userService.createUser(buildCreateUserRequest());

            assertThatThrownBy(() -> userService.createUser(buildCreateUserRequest()))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("Get Users")
    class GetUsers {

        @Test
        @DisplayName("should return all users")
        void getUsers_success() {
            userService.createUser(buildCreateUserRequest());
            List<UserResponse> users = userService.getUsers();
            assertThat(users).isNotEmpty();
        }

        @Test
        @DisplayName("should get current user by ID")
        void getCurrentUser_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());
            UserResponse found = userService.getCurrentUser(created.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getEmail()).isEqualTo(testEmail);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid user ID")
        void getCurrentUser_notFound() {
            UUID randomId = UUID.randomUUID();
            assertThatThrownBy(() -> userService.getCurrentUser(randomId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should get user by email")
        void getUserByEmail_success() {
            userService.createUser(buildCreateUserRequest());
            UserResponse found = userService.getUserByEmail(testEmail);

            assertThat(found).isNotNull();
            assertThat(found.getEmail()).isEqualTo(testEmail);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid email")
        void getUserByEmail_notFound() {
            assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update User")
    class UpdateUser {

        @Test
        @DisplayName("should update user fields")
        void updateUser_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());

            UpdateUserRequest updateRequest = new UpdateUserRequest();
            updateRequest.setFirstName("Jane");
            updateRequest.setLastName("Smith");
            updateRequest.setPhoneNumber("9876543210");

            UserResponse updated = userService.updateUser(created.getId(), updateRequest);

            assertThat(updated.getFirstName()).isEqualTo("Jane");
            assertThat(updated.getLastName()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid user ID")
        void updateUser_notFound() {
            UpdateUserRequest updateRequest = new UpdateUserRequest();
            updateRequest.setFirstName("Jane");

            assertThatThrownBy(() -> userService.updateUser(UUID.randomUUID(), updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete User")
    class DeleteUser {

        @Test
        @DisplayName("should soft delete user")
        void deleteUser_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());
            userService.deleteUser(created.getId());

            User user = userRepository.findById(created.getId()).orElseThrow();
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid user ID")
        void deleteUser_notFound() {
            assertThatThrownBy(() -> userService.deleteUser(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Assign and Remove Roles")
    class RoleAssignment {

        @Test
        @DisplayName("should assign role to user")
        void assignRole_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());
            Role role = roleRepository.findByName("CANDIDATE").orElseThrow();

            // Create a new role to assign
            Role interviewerRole = roleRepository.save(Role.builder()
                    .name("INTERVIEWER_TEST_" + UUID.randomUUID().toString().substring(0, 8))
                    .description("Test interviewer role")
                    .createdAt(Instant.now())
                    .build());

            RoleResponse assigned = userService.assignRoleToUser(created.getId(), interviewerRole.getId());
            assertThat(assigned).isNotNull();
            assertThat(assigned.getName()).isEqualTo(interviewerRole.getName());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when role already assigned")
        void assignRole_duplicate() {
            UserResponse created = userService.createUser(buildCreateUserRequest());
            Role role = roleRepository.findByName("CANDIDATE").orElseThrow();

            assertThatThrownBy(() -> userService.assignRoleToUser(created.getId(), role.getId()))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should get user roles")
        void getUserRoles_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());
            List<RoleResponse> roles = userService.getUserRoles(created.getId());

            assertThat(roles).isNotEmpty();
            assertThat(roles).anyMatch(r -> r.getName().equals("CANDIDATE"));
        }

        @Test
        @DisplayName("should remove role from user")
        void removeRole_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());

            // Add a second role first
            Role extraRole = roleRepository.save(Role.builder()
                    .name("EXTRA_ROLE_" + UUID.randomUUID().toString().substring(0, 8))
                    .description("Extra test role")
                    .createdAt(Instant.now())
                    .build());
            userService.assignRoleToUser(created.getId(), extraRole.getId());

            // Remove the extra role
            userService.removeRoleFromUser(created.getId(), extraRole.getId());

            List<RoleResponse> roles = userService.getUserRoles(created.getId());
            assertThat(roles).noneMatch(r -> r.getName().equals(extraRole.getName()));
        }
    }

    @Nested
    @DisplayName("Change Password")
    class ChangePassword {

        @Test
        @DisplayName("should change password successfully")
        void changePassword_success() {
            // Create user with encoded password
            User user = User.builder()
                    .firstName("Pass")
                    .lastName("Change")
                    .email(testEmail)
                    .password(passwordEncoder.encode("OldPassword@123"))
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            user = userRepository.save(user);

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("OldPassword@123")
                    .newPassword("NewPassword@456")
                    .build();

            userService.changePassword(user.getId(), request);

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(passwordEncoder.matches("NewPassword@456", updated.getPassword())).isTrue();
        }

        @Test
        @DisplayName("should throw BadRequestException for incorrect old password")
        void changePassword_wrongOldPassword() {
            User user = User.builder()
                    .firstName("Pass")
                    .lastName("Change")
                    .email(testEmail)
                    .password(passwordEncoder.encode("OldPassword@123"))
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            user = userRepository.save(user);

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("WrongPassword")
                    .newPassword("NewPassword@456")
                    .build();

            UUID userId = user.getId();
            assertThatThrownBy(() -> userService.changePassword(userId, request))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Search Users")
    class SearchUsers {

        @Test
        @DisplayName("should search users by keyword")
        void searchByKeyword() {
            userService.createUser(buildCreateUserRequest());

            UserSearchRequest request = new UserSearchRequest();
            request.setKeyword("John");
            request.setPage(0);
            request.setSize(10);

            PaginatedResponse<UserResponse> result = userService.searchUsers(request);
            assertThat(result.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("should search users with pagination")
        void searchWithPagination() {
            userService.createUser(buildCreateUserRequest());

            UserSearchRequest request = new UserSearchRequest();
            request.setPage(0);
            request.setSize(5);

            PaginatedResponse<UserResponse> result = userService.searchUsers(request);
            assertThat(result).isNotNull();
            assertThat(result.getSize()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Update User Status")
    class UpdateUserStatus {

        @Test
        @DisplayName("should update user status")
        void updateStatus_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());
            UserResponse updated = userService.updateUserStatus(created.getId(), UserStatus.SUSPENDED);

            assertThat(updated.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("Get User Permissions")
    class GetUserPermissions {

        @Test
        @DisplayName("should return permissions for user with roles")
        void getUserPermissions_success() {
            UserResponse created = userService.createUser(buildCreateUserRequest());
            List<PermissionResponse> permissions = userService.getUserPermissions(created.getId());
            // May be empty if no permissions assigned to role, but should not throw
            assertThat(permissions).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid user ID")
        void getUserPermissions_notFound() {
            assertThatThrownBy(() -> userService.getUserPermissions(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}



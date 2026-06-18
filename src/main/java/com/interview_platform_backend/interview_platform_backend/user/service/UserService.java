package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.user.dto.request.ChangePasswordRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreateUserRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UpdateUserRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UpdateUserProfileRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UserSearchRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserProfileResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;

import java.util.List;
import java.util.UUID;



public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getUsers();

    UserResponse getCurrentUser(UUID userId);

    UserResponse getUserByEmail(String email);

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    void deleteUser(UUID userId);

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(
            UUID userId,
            UpdateUserProfileRequest request
    );

    RoleResponse assignRoleToUser(UUID userId, UUID roleId);

    List<RoleResponse> getUserRoles(UUID userId);

    void removeRoleFromUser(UUID userId, UUID roleId);

    List<PermissionResponse> getUserPermissions(UUID userId);

    List<RoleResponse> getRoles();

    // --- Missing functionalities added below ---

    void changePassword(UUID userId, ChangePasswordRequest request);

    PaginatedResponse<UserResponse> searchUsers(UserSearchRequest request);

    UserResponse updateUserStatus(UUID userId, UserStatus status);
}
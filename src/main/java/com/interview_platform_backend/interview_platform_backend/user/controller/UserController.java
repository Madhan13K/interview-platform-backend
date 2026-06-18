package com.interview_platform_backend.interview_platform_backend.user.controller;


import com.interview_platform_backend.interview_platform_backend.user.dto.request.ChangePasswordRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreateUserRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.AssignRoleRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UpdateUserRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UpdateUserProfileRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.UserSearchRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserProfileResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a new user", description = "Creates a user with default CANDIDATE role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('INTERVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity.ok(
                userService.createUser(request)
        );
    }

    @Operation(summary = "Get current authenticated user")
    @ApiResponse(responseCode = "200", description = "Current user details")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @Operation(summary = "Get all users", description = "Admin only — returns all users")
    @ApiResponse(responseCode = "200", description = "List of all users")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getUsers()
        );
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.username")
    public ResponseEntity<UserResponse> getCurrentUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                userService.getCurrentUser(userId)
        );
    }

    @Operation(summary = "Update user details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.username")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId, @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(
                userService.updateUser(userId, request)
        );
    }

    @Operation(summary = "Delete user (soft delete)", description = "Sets user status to DELETED")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update user profile")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @PutMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CANDIDATE')")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable UUID userId, @RequestBody @Valid UpdateUserProfileRequest request) {
        return ResponseEntity.ok(
                userService.updateProfile(
                        userId,
                        request
                )
        );
    }

    @Operation(summary = "Get user profile by user ID")
    @ApiResponse(responseCode = "200", description = "User profile")
    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CANDIDATE')")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                userService.getProfile(userId)
        );
    }

    @Operation(summary = "Get user permissions")
    @ApiResponse(responseCode = "200", description = "List of permissions")
    @GetMapping("/{userId}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CANDIDATE')")
    public ResponseEntity<List<PermissionResponse>> getUserPermissions(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                userService.getUserPermissions(userId)
        );
    }

    @Operation(summary = "Assign a role to user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned"),
            @ApiResponse(responseCode = "409", description = "Role already assigned")
    })
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CANDIDATE')")
    public ResponseEntity<RoleResponse> assignRoleToUser(@PathVariable UUID userId, @RequestBody @Valid AssignRoleRequest request) {
        return ResponseEntity.ok(
                userService.assignRoleToUser(userId, request.getRoleId())
        );
    }

    @Operation(summary = "Get user roles")
    @ApiResponse(responseCode = "200", description = "List of user roles")
    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                userService.getUserRoles(userId)
        );
    }

    @Operation(summary = "Remove a role from user")
    @ApiResponse(responseCode = "204", description = "Role removed")
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeRoleFromUser(@PathVariable UUID userId, @PathVariable UUID roleId) {
        userService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all available roles")
    @ApiResponse(responseCode = "200", description = "List of roles")
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> getRoles() {
        return ResponseEntity.ok(
                userService.getRoles()
        );
    }

    @Operation(summary = "Change user password")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Old password incorrect"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{userId}/change-password")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isCurrentUser(#userId)")
    public ResponseEntity<Void> changePassword(@PathVariable UUID userId,
                                               @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search users with pagination", description = "Search by keyword and/or status with pagination")
    @ApiResponse(responseCode = "200", description = "Paginated user results")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<UserResponse>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        UserSearchRequest request = UserSearchRequest.builder()
                .keyword(keyword)
                .status(status)
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(userService.searchUsers(request));
    }

    @Operation(summary = "Update user account status", description = "Activate, deactivate, or suspend a user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable UUID userId,
                                                         @RequestParam UserStatus status) {
        return ResponseEntity.ok(userService.updateUserStatus(userId, status));
    }
}

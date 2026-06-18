package com.interview_platform_backend.interview_platform_backend.user.service;

import com.interview_platform_backend.interview_platform_backend.audit.AuditAction;
import com.interview_platform_backend.interview_platform_backend.audit.AuditService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
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
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.RolePermission;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserRole;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserProfile;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.mapper.PermissionMapper;
import com.interview_platform_backend.interview_platform_backend.user.mapper.RoleMapper;
import com.interview_platform_backend.interview_platform_backend.user.mapper.UserMapper;
import com.interview_platform_backend.interview_platform_backend.user.mapper.UserProfileMapper;
import com.interview_platform_backend.interview_platform_backend.user.repository.RolePermissionRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserProfileRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuditService auditService;

    private final UserMapper userMapper;

    private final UserProfileMapper userProfileMapper;

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    public UserServiceImpl(UserRepository userRepository, UserProfileRepository userProfileRepository,
                           RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                           RolePermissionRepository rolePermissionRepository, PasswordEncoder passwordEncoder,
                           AuditService auditService,
                           UserMapper userMapper, UserProfileMapper userProfileMapper,
                           RoleMapper roleMapper, PermissionMapper permissionMapper) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role defaultRole = roleRepository
                .findByName("CANDIDATE")
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role", "name", "CANDIDATE"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .status(UserStatus.ACTIVE)
                .phoneNumber(request.getPhoneNumber())
                .createdAt(Instant.now())
                .build();

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(defaultRole)
                .build();

        user.setUserRoles(new ArrayList<>(List.of(userRole)));
        User savedUser = userRepository.save(user);

        UserProfile userProfile = UserProfile.builder()
                .user(savedUser)
                .build();
        userProfileRepository.save(userProfile);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "users", key = "#userId")
    public UserResponse getCurrentUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.toResponse(user);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "userProfiles", key = "#userId")
    })
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userProfiles", allEntries = true)
    })
    public void deleteUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        // Soft delete: mark as DELETED instead of removing from DB
        user.setStatus(UserStatus.DELETED);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditService.log("User", userId, AuditAction.DELETE, "User soft-deleted");
    }

    @Override
    @Cacheable(value = "userProfiles", key = "#userId")
    public UserProfileResponse getProfile(UUID userId) {

        UserProfile profile = userProfileRepository.findByProfile(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Profile", "userId", userId));
        User user = profile.getUser();

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return userProfileMapper.toResponse(profile,user, roles);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "userProfiles", key = "#userId")
    })
    public UserProfileResponse updateProfile(
            UUID userId,
            UpdateUserProfileRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        UserProfile profile = userProfileRepository
                .findByUser(user)
                .orElse(new UserProfile());

        profile.setUser(user);
        profile.setBio(request.getBio());
        profile.setDesignation(request.getDesignation());
        profile.setCompany(request.getCompany());
        profile.setExperienceYears(
                request.getExperienceYears()
        );
        profile.setGithubUrl(request.getGithubUrl());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setResumeUrl(request.getResumeUrl());

        UserProfile savedProfile =
                userProfileRepository.save(profile);

        if (request.getRoleList() != null && !request.getRoleList().isEmpty()) {
            // Remove existing roles
            userRoleRepository.deleteByUser(user);

            // Assign new roles
            List<UserRole> newRoles = request.getRoleList().stream()
                    .map(roleName -> roleRepository.findByName(roleName.getName())
                            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName.getName())))
                    .map(role -> UserRole.builder()
                            .user(user)
                            .role(role)
                            .assignedAt(Instant.now())
                            .build())
                    .toList();
            userRoleRepository.saveAll(newRoles);
        }
        List<String> roles = userRoleRepository.findByUser(user).stream()
                .map(ur -> ur.getRole().getName())
                .toList();
        return userProfileMapper.toResponse(savedProfile, user, roles);
    }

    @Override
    public RoleResponse assignRoleToUser(UUID userId, UUID roleId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role", "id", roleId));

        if (userRoleRepository.findByUserAndRole(user, role).isPresent()) {
            throw new DuplicateResourceException("Role already assigned to user");
        }

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .assignedAt(Instant.now())
                .build();

        userRoleRepository.save(userRole);

        auditService.log("User", userId, AuditAction.ASSIGN_ROLE,
                "Role '" + role.getName() + "' assigned");

        return roleMapper.toResponse(role);
    }

    @Override
    public List<RoleResponse> getUserRoles(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        return userRoleRepository.findByUser(user)
                .stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public void removeRoleFromUser(UUID userId, UUID roleId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role", "id", roleId));

        UserRole userRole = userRoleRepository.findByUserAndRole(user, role)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role assignment not found"));

        userRoleRepository.delete(userRole);

        auditService.log("User", userId, AuditAction.REMOVE_ROLE,
                "Role '" + role.getName() + "' removed");
    }

    @Override
    public List<RoleResponse> getRoles() {

        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public List<PermissionResponse> getUserPermissions(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        List<Role> roles = userRoleRepository.findByUser(user)
                .stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .toList();

        if (roles.isEmpty()) {
            return List.of();
        }

        return rolePermissionRepository.findByRoleIn(roles)
                .stream()
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        permission -> permission.getId(),
                        permission -> permission,
                        (left, right) -> left
                ))
                .values()
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditService.log("User", userId, AuditAction.PASSWORD_CHANGE, "Password changed");
    }

    @Override
    public PaginatedResponse<UserResponse> searchUsers(UserSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<User> userPage;
        if (request.getKeyword() != null && !request.getKeyword().isBlank() && request.getStatus() != null) {
            userPage = userRepository.searchByKeywordAndStatus(request.getKeyword(), request.getStatus(), pageable);
        } else if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            userPage = userRepository.searchByKeyword(request.getKeyword(), pageable);
        } else if (request.getStatus() != null) {
            userPage = userRepository.findByStatus(request.getStatus(), pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<UserResponse> content = userPage.getContent().stream()
                .map(userMapper::toResponse)
                .toList();

        return PaginatedResponse.<UserResponse>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    public UserResponse updateUserStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserStatus previousStatus = user.getStatus();
        user.setStatus(status);
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);

        auditService.log("User", userId, AuditAction.STATUS_CHANGE,
                "Status changed from " + previousStatus + " to " + status);

        return userMapper.toResponse(savedUser);
    }
}
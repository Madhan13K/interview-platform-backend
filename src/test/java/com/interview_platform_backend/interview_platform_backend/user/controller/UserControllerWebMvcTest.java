package com.interview_platform_backend.interview_platform_backend.user.controller;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserProfileResponse;

class UserControllerWebMvcTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        UserController userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createUser_returnsOk() throws Exception {
        String body = """
                {
                  "firstName": "John",
                  "lastName": "Doe",
                  "email": "john@demo.com",
                  "password": "Secret123",
                  "phoneNumber": "99999"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(userService).createUser(any());
    }

    @Test
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        String body = """
                {
                  "firstName": "John",
                  "lastName": "Doe",
                  "email": "john@demo.com",
                  "password": "Secret123"
                }
                """;

        given(userService.createUser(any()))
                .willThrow(new DuplicateResourceException("User", "email", "john@demo.com"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllUsers_returnsOk() throws Exception {
        given(userService.getUsers()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());

        verify(userService).getUsers();
    }

    @Test
    void getCurrentUser_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userService.getCurrentUser(userId))
                .willThrow(new ResourceNotFoundException("User", "id", userId));

        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = """
                {
                  "firstName": "Jane",
                  "status": "ACTIVE"
                }
                """;

        mockMvc.perform(put("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(userId), any());
    }

    @Test
    void updateUser_withInvalidStatus_returnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = """
                {
                  "status": "WRONG_STATUS"
                }
                """;

        mockMvc.perform(put("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_returnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("User", "id", userId))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfile_missing_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userService.getProfile(userId))
                .willThrow(new ResourceNotFoundException("Profile not found"));

        mockMvc.perform(get("/api/v1/users/{userId}/profile", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Profile not found"));
    }

    @Test
    void getProfile_existingProfile_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john@demo.com")
                .bio("5 years exp")
                .designation("SDE")
                .company("ABC")
                .build();

        given(userService.getProfile(userId)).willReturn(profileResponse);

        mockMvc.perform(get("/api/v1/users/{userId}/profile", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.bio").value("5 years exp"));
    }

    @Test
    void updateProfile_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = """
                {
                  "bio": "5 years exp",
                  "designation": "SDE",
                  "company": "ABC"
                }
                """;

        mockMvc.perform(put("/api/v1/users/{userId}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq(userId), any());
    }

    @Test
    void updateProfile_existingProfile_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = """
                {
                  "bio": "Updated bio",
                  "designation": "Senior SDE",
                  "company": "XYZ Corp",
                  "experienceYears": 7
                }
                """;

        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .userId(userId)
                .bio("Updated bio")
                .designation("Senior SDE")
                .company("XYZ Corp")
                .experienceYears(7)
                .build();

        given(userService.updateProfile(eq(userId), any())).willReturn(profileResponse);

        mockMvc.perform(put("/api/v1/users/{userId}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.experienceYears").value(7));
    }

    @Test
    void assignRole_duplicate_returnsConflict() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        String body = """
                {
                  "roleId": "%s"
                }
                """.formatted(roleId);

        given(userService.assignRoleToUser(userId, roleId))
                .willThrow(new DuplicateResourceException("Role already assigned to user"));

        mockMvc.perform(post("/api/v1/users/{userId}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void assignRole_success_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        String body = """
                {
                  "roleId": "%s"
                }
                """.formatted(roleId);

        RoleResponse roleResponse = RoleResponse.builder()
                .id(roleId)
                .name("ADMIN")
                .description("Admin role")
                .build();

        given(userService.assignRoleToUser(userId, roleId)).willReturn(roleResponse);

        mockMvc.perform(post("/api/v1/users/{userId}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ADMIN"));
    }

    @Test
    void assignRole_userNotFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        String body = """
                {
                  "roleId": "%s"
                }
                """.formatted(roleId);

        given(userService.assignRoleToUser(userId, roleId))
                .willThrow(new ResourceNotFoundException("User", "id", userId));

        mockMvc.perform(post("/api/v1/users/{userId}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignRole_roleNotFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        String body = """
                {
                  "roleId": "%s"
                }
                """.formatted(roleId);

        given(userService.assignRoleToUser(userId, roleId))
                .willThrow(new ResourceNotFoundException("Role", "id", roleId));

        mockMvc.perform(post("/api/v1/users/{userId}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeUserRole_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Role assignment not found"))
                .when(userService).removeRoleFromUser(userId, roleId);

        mockMvc.perform(delete("/api/v1/users/{userId}/roles/{roleId}", userId, roleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeRoleFromUser_success_returnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        doNothing().when(userService).removeRoleFromUser(userId, roleId);

        mockMvc.perform(delete("/api/v1/users/{userId}/roles/{roleId}", userId, roleId))
                .andExpect(status().isNoContent());

        verify(userService).removeRoleFromUser(userId, roleId);
    }

    @Test
    void getUserRoles_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userService.getUserRoles(userId)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/users/{userId}/roles", userId))
                .andExpect(status().isOk());

        verify(userService).getUserRoles(userId);
    }

    @Test
    void getUserPermissions_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userService.getUserPermissions(userId)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/users/{userId}/permissions", userId))
                .andExpect(status().isOk());

        verify(userService).getUserPermissions(userId);
    }

    @Test
    void invalidUuidPath_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}


package com.interview_platform_backend.interview_platform_backend.user.controller;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.service.RolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolePermissionControllerWebMvcTest {

    private MockMvc mockMvc;
    private RolePermissionService rolePermissionService;

    @BeforeEach
    void setUp() {
        rolePermissionService = mock(RolePermissionService.class);
        RolePermissionController controller = new RolePermissionController(rolePermissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void assignPermissionToRole_returnsOk() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        String body = """
                {
                  "permissionId": "%s"
                }
                """.formatted(permissionId);

        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(rolePermissionService).assignPermissionToRole(eq(roleId), eq(permissionId));
    }

    @Test
    void assignPermissionToRole_duplicate_returnsConflict() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        String body = """
                {
                  "permissionId": "%s"
                }
                """.formatted(permissionId);

        given(rolePermissionService.assignPermissionToRole(roleId, permissionId))
                .willThrow(new DuplicateResourceException("Permission already assigned to role"));

        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Permission already assigned to role"));
    }

    @Test
    void assignPermissionToRole_roleNotFound_returns404() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        String body = """
                {
                  "permissionId": "%s"
                }
                """.formatted(permissionId);

        given(rolePermissionService.assignPermissionToRole(roleId, permissionId))
                .willThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPermissionsByRole_returnsOk() throws Exception {
        UUID roleId = UUID.randomUUID();
        given(rolePermissionService.getPermissionsByRole(roleId)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/roles/{roleId}/permissions", roleId))
                .andExpect(status().isOk());

        verify(rolePermissionService).getPermissionsByRole(roleId);
    }

    @Test
    void removePermissionFromRole_returnsNoContent() throws Exception {
        UUID rolePermissionId = UUID.randomUUID();
        doNothing().when(rolePermissionService).removePermissionFromRole(rolePermissionId);

        mockMvc.perform(delete("/api/v1/roles/permissions/{rolePermissionId}", rolePermissionId))
                .andExpect(status().isNoContent());

        verify(rolePermissionService).removePermissionFromRole(rolePermissionId);
    }

    @Test
    void removePermissionFromRole_notFound_returns404() throws Exception {
        UUID rolePermissionId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("RolePermission not found"))
                .when(rolePermissionService).removePermissionFromRole(rolePermissionId);

        mockMvc.perform(delete("/api/v1/roles/permissions/{rolePermissionId}", rolePermissionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignPermissionToRole_permissionNotFound_returns404() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        String body = """
                {
                  "permissionId": "%s"
                }
                """.formatted(permissionId);

        given(rolePermissionService.assignPermissionToRole(roleId, permissionId))
                .willThrow(new ResourceNotFoundException("Permission not found"));

        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}


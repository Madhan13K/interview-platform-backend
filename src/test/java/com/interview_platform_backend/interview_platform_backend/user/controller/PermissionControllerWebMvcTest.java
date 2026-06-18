package com.interview_platform_backend.interview_platform_backend.user.controller;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

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

class PermissionControllerWebMvcTest {

    private MockMvc mockMvc;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {

        permissionService = mock(PermissionService.class);
        PermissionController permissionController = new PermissionController(permissionService);

        mockMvc = MockMvcBuilders.standaloneSetup(permissionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPermission_returnsOk() throws Exception {

        String requestBody = """
                {
                  "name": "VIEW_USERS",
                  "description": "Can view users"
                }
                """;

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(permissionService).createPermission(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createPermission_withMissingName_returnsBadRequest() throws Exception {

        String requestBody = """
                {
                  "description": "Invalid because name is missing"
                }
                """;

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPermission_whenDuplicate_returnsConflict() throws Exception {

        String requestBody = """
                {
                  "name": "VIEW_USERS",
                  "description": "Can view users"
                }
                """;

        given(permissionService.createPermission(org.mockito.ArgumentMatchers.any()))
                .willThrow(new DuplicateResourceException("Permission already exists"));

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Permission already exists"));
    }

    @Test
    void getAllPermissions_returnsOk() throws Exception {

        given(permissionService.getAllPermissions()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk());

        verify(permissionService).getAllPermissions();
    }

    @Test
    void getPermissionById_whenNotFound_returns404() throws Exception {

        UUID permissionId = UUID.randomUUID();
        given(permissionService.getPermissionById(permissionId))
                .willThrow(new ResourceNotFoundException("Permission", "id", permissionId));

        mockMvc.perform(get("/api/v1/permissions/{permissionId}", permissionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePermission_returnsOk() throws Exception {

        UUID permissionId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "EDIT_USERS",
                  "description": "Can edit users"
                }
                """;

        mockMvc.perform(put("/api/v1/permissions/{permissionId}", permissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(permissionService).updatePermission(org.mockito.ArgumentMatchers.eq(permissionId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletePermission_returnsNoContent() throws Exception {

        UUID permissionId = UUID.randomUUID();
        doNothing().when(permissionService).deletePermission(permissionId);

        mockMvc.perform(delete("/api/v1/permissions/{permissionId}", permissionId))
                .andExpect(status().isNoContent());

        verify(permissionService).deletePermission(permissionId);
    }

    @Test
    void updatePermission_duplicateName_returnsConflict() throws Exception {

        UUID permissionId = UUID.randomUUID();
        String requestBody = """
                {
                  "name": "EXISTING_PERMISSION",
                  "description": "Trying to rename to an existing name"
                }
                """;

        given(permissionService.updatePermission(org.mockito.ArgumentMatchers.eq(permissionId), org.mockito.ArgumentMatchers.any()))
                .willThrow(new DuplicateResourceException("Permission", "name", "EXISTING_PERMISSION"));

        mockMvc.perform(put("/api/v1/permissions/{permissionId}", permissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void deletePermission_notFound_returns404() throws Exception {

        UUID permissionId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Permission", "id", permissionId))
                .when(permissionService).deletePermission(permissionId);

        mockMvc.perform(delete("/api/v1/permissions/{permissionId}", permissionId))
                .andExpect(status().isNotFound());
    }
}



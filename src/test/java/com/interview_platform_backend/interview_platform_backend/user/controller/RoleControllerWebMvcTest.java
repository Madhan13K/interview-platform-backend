
package com.interview_platform_backend.interview_platform_backend.user.controller;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.service.RoleService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerWebMvcTest {

    private MockMvc mockMvc;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = mock(RoleService.class);
        RoleController roleController = new RoleController(roleService);
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createRole_returnsOk() throws Exception {
        String body = """
                {
                  "name": "ADMIN",
                  "description": "Admin role"
                }
                """;

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(roleService).createRole(any());
    }

    @Test
    void createRole_withInvalidBody_returnsBadRequest() throws Exception {
        String body = """
                {
                  "description": "Missing name"
                }
                """;

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_whenDuplicate_returnsConflict() throws Exception {
        String body = """
                {
                  "name": "ADMIN",
                  "description": "Admin role"
                }
                """;

        given(roleService.createRole(any()))
                .willThrow(new DuplicateResourceException("Role already exists"));

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Role already exists"));
    }

    @Test
    void getAllRoles_returnsOk() throws Exception {
        given(roleService.getAllRoles()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk());

        verify(roleService).getAllRoles();
    }

    @Test
    void getRoleById_whenNotFound_returns404() throws Exception {
        UUID roleId = UUID.randomUUID();
        given(roleService.getRoleById(roleId))
                .willThrow(new ResourceNotFoundException("Role", "id", roleId));

        mockMvc.perform(get("/api/v1/roles/{roleId}", roleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRole_returnsOk() throws Exception {
        UUID roleId = UUID.randomUUID();
        String body = """
                {
                  "name": "RECRUITER",
                  "description": "Recruiter role"
                }
                """;

        mockMvc.perform(put("/api/v1/roles/{roleId}", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(roleService).updateRole(eq(roleId), any());
    }

    @Test
    void deleteRole_returnsNoContent() throws Exception {
        UUID roleId = UUID.randomUUID();
        doNothing().when(roleService).deleteRole(roleId);

        mockMvc.perform(delete("/api/v1/roles/{roleId}", roleId))
                .andExpect(status().isNoContent());

        verify(roleService).deleteRole(roleId);
    }

    @Test
    void deleteRole_whenNotFound_returns404() throws Exception {
        UUID roleId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Role", "id", roleId))
                .when(roleService).deleteRole(roleId);

        mockMvc.perform(delete("/api/v1/roles/{roleId}", roleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRole_duplicateName_returnsConflict() throws Exception {
        UUID roleId = UUID.randomUUID();
        String body = """
                {
                  "name": "EXISTING_ROLE",
                  "description": "Trying to rename to an existing role"
                }
                """;

        given(roleService.updateRole(eq(roleId), any()))
                .willThrow(new DuplicateResourceException("Role", "name", "EXISTING_ROLE"));

        mockMvc.perform(put("/api/v1/roles/{roleId}", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}


package com.interview_platform_backend.interview_platform_backend.user.controller;

import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreateRoleRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import com.interview_platform_backend.interview_platform_backend.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Role management endpoints")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Create a new role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> createRole(@RequestBody @Valid CreateRoleRequest request) {
        return ResponseEntity.ok(
                roleService.createRole(request)
        );
    }

    @Operation(summary = "Get all roles")
    @ApiResponse(responseCode = "200", description = "List of all roles")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(
                roleService.getAllRoles()
        );
    }

    @Operation(summary = "Get role by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role found"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @GetMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CANDIDATE') or hasRole('INTERVIEWER')")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID roleId) {
        return ResponseEntity.ok(
                roleService.getRoleById(roleId)
        );
    }

    @Operation(summary = "Update a role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable UUID roleId, @RequestBody @Valid CreateRoleRequest request) {
        return ResponseEntity.ok(
                roleService.updateRole(roleId, request)
        );
    }

    @Operation(summary = "Delete a role")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role deleted"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }
}
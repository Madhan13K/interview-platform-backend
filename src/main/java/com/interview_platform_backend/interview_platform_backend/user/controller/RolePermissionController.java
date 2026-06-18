package com.interview_platform_backend.interview_platform_backend.user.controller;

import com.interview_platform_backend.interview_platform_backend.user.dto.request.AssignPermissionRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.RolePermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.service.RolePermissionService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Role Permissions", description = "Manage permissions assigned to roles")
public class RolePermissionController {

    private final RolePermissionService
            rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Operation(summary = "Assign permission to a role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission assigned to role"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found")
    })
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolePermissionResponse>
    assignPermissionToRole(@PathVariable UUID roleId, @RequestBody @Valid AssignPermissionRequest request) {
        return ResponseEntity.ok(
                rolePermissionService
                        .assignPermissionToRole(
                                roleId,
                                request.getPermissionId()
                        )
        );
    }

    @Operation(summary = "Get all permissions for a role")
    @ApiResponse(responseCode = "200", description = "List of role permissions")
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RolePermissionResponse>> getPermissionsByRole(
            @PathVariable UUID roleId
    ) {

        return ResponseEntity.ok(
                rolePermissionService.getPermissionsByRole(roleId)
        );
    }

    @Operation(summary = "Remove permission from a role")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permission removed from role"),
            @ApiResponse(responseCode = "404", description = "Role permission not found")
    })
    @DeleteMapping("/permissions/{rolePermissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable UUID rolePermissionId
    ) {

        rolePermissionService.removePermissionFromRole(rolePermissionId);

        return ResponseEntity.noContent().build();
    }
}
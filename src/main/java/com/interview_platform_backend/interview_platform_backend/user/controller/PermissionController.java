package com.interview_platform_backend.interview_platform_backend.user.controller;


import com.interview_platform_backend.interview_platform_backend.user.dto.request.CreatePermissionRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PermissionResponse;
import com.interview_platform_backend.interview_platform_backend.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions", description = "Permission management endpoints")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Create a new permission")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> createPermission(@RequestBody @Valid CreatePermissionRequest request) {
        return ResponseEntity.ok(
                permissionService.createPermission(request)
        );
    }

    @Operation(summary = "Get all permissions")
    @ApiResponse(responseCode = "200", description = "List of all permissions")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(
                permissionService.getAllPermissions()
        );
    }

    @Operation(summary = "Get permission by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission found"),
            @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    @GetMapping("/{permissionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CANDIDATE')")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable UUID permissionId) {
        return ResponseEntity.ok(
                permissionService.getPermissionById(permissionId)
        );
    }

    @Operation(summary = "Update a permission")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission updated"),
            @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    @PutMapping("/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> updatePermission(@PathVariable UUID permissionId, @RequestBody @Valid CreatePermissionRequest request) {
        return ResponseEntity.ok(
                permissionService.updatePermission(permissionId, request)
        );
    }

    @Operation(summary = "Delete a permission")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permission deleted"),
            @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }
}

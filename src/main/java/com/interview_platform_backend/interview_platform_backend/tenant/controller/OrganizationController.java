package com.interview_platform_backend.interview_platform_backend.tenant.controller;

import com.interview_platform_backend.interview_platform_backend.tenant.dto.*;
import com.interview_platform_backend.interview_platform_backend.tenant.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Multi-tenant organization management")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new organization", description = "Creates a new organization and assigns the current user as owner")
    @ApiResponse(responseCode = "201", description = "Organization created successfully")
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.createOrganization(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID", description = "Retrieves organization details by its ID")
    @ApiResponse(responseCode = "200", description = "Organization retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getOrganization(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update organization", description = "Updates organization details")
    @ApiResponse(responseCode = "200", description = "Organization updated successfully")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete organization", description = "Deletes an organization and all its members")
    @ApiResponse(responseCode = "204", description = "Organization deleted successfully")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's organizations", description = "Retrieves all organizations the current user belongs to")
    @ApiResponse(responseCode = "200", description = "Organizations retrieved successfully")
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations() {
        return ResponseEntity.ok(organizationService.getOrganizationsByUser());
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to organization", description = "Adds a user as a member of the organization")
    @ApiResponse(responseCode = "201", description = "Member added successfully")
    @ApiResponse(responseCode = "404", description = "Organization or user not found")
    @ApiResponse(responseCode = "409", description = "User is already a member")
    public ResponseEntity<OrganizationMemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.addMember(id, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member from organization", description = "Removes a user from the organization")
    @ApiResponse(responseCode = "204", description = "Member removed successfully")
    @ApiResponse(responseCode = "404", description = "Organization or member not found")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        organizationService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List organization members", description = "Retrieves all members of the organization")
    @ApiResponse(responseCode = "200", description = "Members retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<List<OrganizationMemberResponse>> getMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getMembers(id));
    }

    @PatchMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Update member role", description = "Updates the role of a member in the organization")
    @ApiResponse(responseCode = "200", description = "Member role updated successfully")
    @ApiResponse(responseCode = "404", description = "Organization or member not found")
    public ResponseEntity<OrganizationMemberResponse> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam String role) {
        return ResponseEntity.ok(organizationService.updateMemberRole(id, userId, role));
    }
}

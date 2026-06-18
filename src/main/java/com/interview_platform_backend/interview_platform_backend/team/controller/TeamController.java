package com.interview_platform_backend.interview_platform_backend.team.controller;

import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.team.dto.*;
import com.interview_platform_backend.interview_platform_backend.team.entity.TeamMember;
import com.interview_platform_backend.interview_platform_backend.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams & Departments", description = "Organize interviewers into teams/departments")
public class TeamController {

    private final TeamService teamService;
    private final SecurityHelper securityHelper;

    public TeamController(TeamService teamService, SecurityHelper securityHelper) {
        this.teamService = teamService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Create a team")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> createTeam(@RequestBody @Valid CreateTeamRequest request) {
        return ResponseEntity.ok(teamService.createTeam(request));
    }

    @Operation(summary = "Get team by ID")
    @GetMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    @Operation(summary = "Get all active teams")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @Operation(summary = "Get teams by department")
    @GetMapping("/department/{department}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamResponse>> getByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(teamService.getTeamsByDepartment(department));
    }

    @Operation(summary = "Get my teams")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamResponse>> getMyTeams() {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(teamService.getMyTeams(userId));
    }

    @Operation(summary = "Update a team")
    @PutMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable UUID teamId, @RequestBody @Valid CreateTeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(teamId, request));
    }

    @Operation(summary = "Deactivate a team")
    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add member to team")
    @PostMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable UUID teamId, @PathVariable UUID userId,
            @RequestParam(defaultValue = "MEMBER") TeamMember.TeamRole role) {
        return ResponseEntity.ok(teamService.addMember(teamId, userId, role));
    }

    @Operation(summary = "Remove member from team")
    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> removeMember(@PathVariable UUID teamId, @PathVariable UUID userId) {
        return ResponseEntity.ok(teamService.removeMember(teamId, userId));
    }

    @Operation(summary = "Update member role")
    @PatchMapping("/{teamId}/members/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> updateMemberRole(
            @PathVariable UUID teamId, @PathVariable UUID userId,
            @RequestParam TeamMember.TeamRole role) {
        return ResponseEntity.ok(teamService.updateMemberRole(teamId, userId, role));
    }
}


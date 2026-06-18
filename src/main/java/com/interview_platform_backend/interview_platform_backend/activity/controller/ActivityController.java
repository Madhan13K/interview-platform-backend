package com.interview_platform_backend.interview_platform_backend.activity.controller;

import com.interview_platform_backend.interview_platform_backend.activity.dto.ActivityEventResponse;
import com.interview_platform_backend.interview_platform_backend.activity.dto.ActivityFilterRequest;
import com.interview_platform_backend.interview_platform_backend.activity.service.ActivityService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
@Tag(name = "Activity Feed", description = "Activity feed and timeline endpoints")
public class ActivityController {

    private final ActivityService activityService;
    private final SecurityHelper securityHelper;

    public ActivityController(ActivityService activityService, SecurityHelper securityHelper) {
        this.activityService = activityService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Get global activity feed")
    @ApiResponse(responseCode = "200", description = "Activity feed retrieved")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<ActivityEventResponse>> getActivityFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(activityService.getActivityFeed(page, size));
    }

    @Operation(summary = "Get timeline for a specific entity")
    @ApiResponse(responseCode = "200", description = "Entity timeline retrieved")
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<List<ActivityEventResponse>> getActivityForEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(activityService.getActivityForEntity(entityType, entityId));
    }

    @Operation(summary = "Get activity by user")
    @ApiResponse(responseCode = "200", description = "User activity retrieved")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<ActivityEventResponse>> getActivityByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(activityService.getActivityByActor(userId, page, size));
    }

    @Operation(summary = "Get current user's activity")
    @ApiResponse(responseCode = "200", description = "My activity retrieved")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<ActivityEventResponse>> getMyActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(activityService.getActivityByActor(currentUserId, page, size));
    }

    @Operation(summary = "Get filtered activity feed")
    @ApiResponse(responseCode = "200", description = "Filtered activity retrieved")
    @PostMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<ActivityEventResponse>> getFilteredActivity(
            @RequestBody ActivityFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(activityService.getFilteredActivity(filter, page, size));
    }
}

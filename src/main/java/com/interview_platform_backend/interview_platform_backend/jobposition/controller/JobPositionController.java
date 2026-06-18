package com.interview_platform_backend.interview_platform_backend.jobposition.controller;

import com.interview_platform_backend.interview_platform_backend.jobposition.dto.*;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPositionStatus;
import com.interview_platform_backend.interview_platform_backend.jobposition.service.JobPositionService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
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
@RequestMapping("/api/v1/job-positions")
@Tag(name = "Job Positions", description = "Job openings management - link interviews to specific positions")
public class JobPositionController {

    private final JobPositionService jobPositionService;
    private final SecurityHelper securityHelper;

    public JobPositionController(JobPositionService jobPositionService, SecurityHelper securityHelper) {
        this.jobPositionService = jobPositionService;
        this.securityHelper = securityHelper;
    }

    // ==================== CRUD ====================

    @Operation(summary = "Create a new job position/opening")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job position created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<JobPositionResponse> createJobPosition(
            @RequestBody @Valid CreateJobPositionRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(jobPositionService.createJobPosition(request, currentUserId));
    }

    @Operation(summary = "Get job position by ID")
    @ApiResponse(responseCode = "200", description = "Job position found")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobPositionResponse> getJobPosition(@PathVariable UUID id) {
        return ResponseEntity.ok(jobPositionService.getJobPosition(id));
    }

    @Operation(summary = "Get all job positions")
    @ApiResponse(responseCode = "200", description = "List of job positions")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobPositionResponse>> getAllJobPositions() {
        return ResponseEntity.ok(jobPositionService.getAllJobPositions());
    }

    @Operation(summary = "Get job positions (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated job positions")
    @GetMapping("/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<JobPositionResponse>> getJobPositionsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobPositionService.getJobPositionsPaginated(page, size));
    }

    @Operation(summary = "Search job positions by keyword")
    @ApiResponse(responseCode = "200", description = "Search results")
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<JobPositionResponse>> searchJobPositions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobPositionService.searchJobPositions(keyword, page, size));
    }

    @Operation(summary = "Filter job positions by status")
    @ApiResponse(responseCode = "200", description = "Filtered job positions")
    @GetMapping("/filter/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobPositionResponse>> getByStatus(@RequestParam JobPositionStatus status) {
        return ResponseEntity.ok(jobPositionService.getJobPositionsByStatus(status));
    }

    @Operation(summary = "Get my created job positions")
    @ApiResponse(responseCode = "200", description = "User's job positions")
    @GetMapping("/my")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<JobPositionResponse>> getMyJobPositions() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(jobPositionService.getMyJobPositions(currentUserId));
    }

    @Operation(summary = "Update a job position")
    @ApiResponse(responseCode = "200", description = "Job position updated")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<JobPositionResponse> updateJobPosition(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateJobPositionRequest request) {
        return ResponseEntity.ok(jobPositionService.updateJobPosition(id, request));
    }

    @Operation(summary = "Update job position status")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<JobPositionResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam JobPositionStatus status) {
        return ResponseEntity.ok(jobPositionService.updateStatus(id, status));
    }

    @Operation(summary = "Delete a job position")
    @ApiResponse(responseCode = "204", description = "Job position deleted")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteJobPosition(@PathVariable UUID id) {
        jobPositionService.deleteJobPosition(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Interview Linking ====================

    @Operation(summary = "Link an interview to a job position")
    @ApiResponse(responseCode = "200", description = "Interview linked")
    @PostMapping("/{positionId}/interviews/{interviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<JobPositionResponse> linkInterview(
            @PathVariable UUID positionId,
            @PathVariable UUID interviewId) {
        return ResponseEntity.ok(jobPositionService.linkInterviewToPosition(positionId, interviewId));
    }

    @Operation(summary = "Unlink an interview from its job position")
    @ApiResponse(responseCode = "204", description = "Interview unlinked")
    @DeleteMapping("/interviews/{interviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<Void> unlinkInterview(@PathVariable UUID interviewId) {
        jobPositionService.unlinkInterviewFromPosition(interviewId);
        return ResponseEntity.noContent().build();
    }
}


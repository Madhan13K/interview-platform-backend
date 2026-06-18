package com.interview_platform_backend.interview_platform_backend.jobboard.controller;

import com.interview_platform_backend.interview_platform_backend.jobboard.dto.ApplicationStatusUpdate;
import com.interview_platform_backend.interview_platform_backend.jobboard.dto.JobApplicationRequest;
import com.interview_platform_backend.interview_platform_backend.jobboard.dto.JobApplicationResponse;
import com.interview_platform_backend.interview_platform_backend.jobboard.service.JobBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portal")
@Tag(name = "Candidate Portal", description = "Candidate application management and recruiter administration")
public class CandidatePortalController {

    private final JobBoardService jobBoardService;

    public CandidatePortalController(JobBoardService jobBoardService) {
        this.jobBoardService = jobBoardService;
    }

    // ==================== Candidate Endpoints ====================

    @Operation(summary = "Submit a job application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate application"),
            @ApiResponse(responseCode = "404", description = "Job position not found")
    })
    @PostMapping("/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<JobApplicationResponse> submitApplication(
            @RequestBody @Valid JobApplicationRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(jobBoardService.submitApplication(request, email));
    }

    @Operation(summary = "Get my applications")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications retrieved")
    })
    @GetMapping("/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<List<JobApplicationResponse>> getMyApplications(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(jobBoardService.getMyApplications(email));
    }

    @Operation(summary = "Get application detail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application detail retrieved"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @GetMapping("/applications/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<JobApplicationResponse> getApplicationDetail(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(jobBoardService.getApplicationDetail(id, email));
    }

    @Operation(summary = "Withdraw an application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application withdrawn"),
            @ApiResponse(responseCode = "400", description = "Cannot withdraw this application"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @DeleteMapping("/applications/{id}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<JobApplicationResponse> withdrawApplication(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(jobBoardService.withdrawApplication(id, email));
    }

    // ==================== Admin/Recruiter Endpoints ====================

    @Operation(summary = "Get all applications for a job position (recruiter view)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications retrieved"),
            @ApiResponse(responseCode = "404", description = "Job position not found")
    })
    @GetMapping("/admin/applications/position/{positionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsForPosition(
            @PathVariable UUID positionId) {
        return ResponseEntity.ok(jobBoardService.getApplicationsForPosition(positionId));
    }

    @Operation(summary = "Update application status (recruiter)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application status updated"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PatchMapping("/admin/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<JobApplicationResponse> updateApplicationStatus(
            @PathVariable UUID id,
            @RequestBody @Valid ApplicationStatusUpdate statusUpdate,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(jobBoardService.updateApplicationStatus(id, statusUpdate, email));
    }
}

package com.interview_platform_backend.interview_platform_backend.dashboard.controller;

import com.interview_platform_backend.interview_platform_backend.dashboard.dto.CandidateDashboard;
import com.interview_platform_backend.interview_platform_backend.dashboard.dto.DashboardStats;
import com.interview_platform_backend.interview_platform_backend.dashboard.dto.InterviewerDashboard;
import com.interview_platform_backend.interview_platform_backend.dashboard.service.DashboardService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics and analytics")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityHelper securityHelper;

    public DashboardController(DashboardService dashboardService, SecurityHelper securityHelper) {
        this.dashboardService = dashboardService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Get admin/recruiter dashboard stats",
            description = "Overall platform statistics: interview counts, user counts, feedback metrics")
    @ApiResponse(responseCode = "200", description = "Dashboard statistics")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<DashboardStats> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminStats());
    }

    @Operation(summary = "Get interviewer dashboard",
            description = "Interviewer-specific stats: assigned interviews, pending feedback, upcoming schedule")
    @ApiResponse(responseCode = "200", description = "Interviewer dashboard")
    @GetMapping("/interviewer")
    @PreAuthorize("hasRole('INTERVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<InterviewerDashboard> getInterviewerDashboard() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getInterviewerDashboard(currentUserId));
    }

    @Operation(summary = "Get interviewer dashboard by ID (admin view)")
    @ApiResponse(responseCode = "200", description = "Interviewer dashboard")
    @GetMapping("/interviewer/{interviewerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<InterviewerDashboard> getInterviewerDashboardById(@PathVariable UUID interviewerId) {
        return ResponseEntity.ok(dashboardService.getInterviewerDashboard(interviewerId));
    }

    @Operation(summary = "Get candidate dashboard",
            description = "Candidate-specific stats: upcoming interviews, completed count, meeting links")
    @ApiResponse(responseCode = "200", description = "Candidate dashboard")
    @GetMapping("/candidate")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<CandidateDashboard> getCandidateDashboard() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getCandidateDashboard(currentUserId));
    }
}


package com.interview_platform_backend.interview_platform_backend.candidate.controller;

import com.interview_platform_backend.interview_platform_backend.candidate.dto.*;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.service.InterviewService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@Tag(name = "Interviews", description = "Interview management endpoints")
public class InterviewController {

    private final InterviewService interviewService;
    private final SecurityHelper securityHelper;

    public InterviewController(InterviewService interviewService, SecurityHelper securityHelper) {
        this.interviewService = interviewService;
        this.securityHelper = securityHelper;
    }

    // ==================== CRUD ====================

    @Operation(summary = "Create a new interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('INTERVIEW_CREATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> createInterview(
            @RequestBody @Valid CreateInterviewRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(interviewService.createInterview(request, currentUserId));
    }

    @Operation(summary = "Get interview by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview found"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @GetMapping("/{interviewId}")
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<InterviewResponse> getInterview(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(interviewService.getInterview(interviewId));
    }

    @Operation(summary = "Get all interviews")
    @ApiResponse(responseCode = "200", description = "List of interviews")
    @GetMapping
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<InterviewResponse>> getInterviews() {
        return ResponseEntity.ok(interviewService.getInterviews());
    }

    @Operation(summary = "Get all interviews (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated interviews")
    @GetMapping("/paginated")
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<InterviewResponse>> getInterviewsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(interviewService.getInterviewsPaginated(page, size));
    }

    @Operation(summary = "Update interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview updated"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @PutMapping("/{interviewId}")
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> updateInterview(
            @PathVariable UUID interviewId,
            @RequestBody @Valid UpdateInterviewRequest request) {
        return ResponseEntity.ok(interviewService.updateInterview(interviewId, request));
    }

    @Operation(summary = "Delete interview")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Interview deleted"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @DeleteMapping("/{interviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInterview(@PathVariable UUID interviewId) {
        interviewService.deleteInterview(interviewId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Status Management ====================

    @Operation(summary = "Cancel an interview")
    @ApiResponse(responseCode = "200", description = "Interview cancelled")
    @PatchMapping("/{interviewId}/cancel")
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> cancelInterview(
            @PathVariable UUID interviewId,
            @RequestBody @Valid CancelInterviewRequest request) {
        return ResponseEntity.ok(interviewService.cancelInterview(interviewId, request));
    }

    @Operation(summary = "Update interview status")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PatchMapping("/{interviewId}/status")
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> updateStatus(
            @PathVariable UUID interviewId,
            @RequestParam InterviewStatus status) {
        return ResponseEntity.ok(interviewService.updateStatus(interviewId, status));
    }

    // ==================== My Interviews ====================

    @Operation(summary = "Get my interviews as candidate")
    @ApiResponse(responseCode = "200", description = "List of candidate's interviews")
    @GetMapping("/my/candidate")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<List<InterviewResponse>> getMyInterviewsAsCandidate() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(interviewService.getMyInterviewsAsCandidate(currentUserId));
    }

    @Operation(summary = "Get my interviews as candidate (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated candidate interviews")
    @GetMapping("/my/candidate/paginated")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<PaginatedResponse<InterviewResponse>> getMyInterviewsAsCandidatePaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(interviewService.getMyInterviewsAsCandidatePaginated(currentUserId, page, size));
    }

    @Operation(summary = "Get my interviews as interviewer")
    @ApiResponse(responseCode = "200", description = "List of interviewer's interviews")
    @GetMapping("/my/interviewer")
    @PreAuthorize("hasRole('INTERVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<List<InterviewResponse>> getMyInterviewsAsInterviewer() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(interviewService.getMyInterviewsAsInterviewer(currentUserId));
    }

    @Operation(summary = "Get my interviews as interviewer (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated interviewer interviews")
    @GetMapping("/my/interviewer/paginated")
    @PreAuthorize("hasRole('INTERVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<PaginatedResponse<InterviewResponse>> getMyInterviewsAsInterviewerPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(interviewService.getMyInterviewsAsInterviewerPaginated(currentUserId, page, size));
    }

    // ==================== Interviewer Management ====================

    @Operation(summary = "Add interviewer to an interview")
    @ApiResponse(responseCode = "200", description = "Interviewer added")
    @PostMapping("/{interviewId}/interviewers/{interviewerId}")
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> addInterviewer(
            @PathVariable UUID interviewId,
            @PathVariable UUID interviewerId,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        return ResponseEntity.ok(interviewService.addInterviewer(interviewId, interviewerId, isPrimary));
    }

    @Operation(summary = "Remove interviewer from an interview")
    @ApiResponse(responseCode = "200", description = "Interviewer removed")
    @DeleteMapping("/{interviewId}/interviewers/{interviewerId}")
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> removeInterviewer(
            @PathVariable UUID interviewId,
            @PathVariable UUID interviewerId) {
        return ResponseEntity.ok(interviewService.removeInterviewer(interviewId, interviewerId));
    }

    // ==================== Feedback ====================

    @Operation(summary = "Submit feedback for an interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback submitted"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @PostMapping("/{interviewId}/feedback")
    @PreAuthorize("hasRole('INTERVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @PathVariable UUID interviewId,
            @RequestBody @Valid SubmitFeedbackRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(interviewService.submitFeedback(interviewId, currentUserId, request));
    }

    @Operation(summary = "Get all feedback for an interview")
    @ApiResponse(responseCode = "200", description = "List of feedback")
    @GetMapping("/{interviewId}/feedback")
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<FeedbackResponse>> getInterviewFeedback(
            @PathVariable UUID interviewId) {
        return ResponseEntity.ok(interviewService.getInterviewFeedback(interviewId));
    }

    @Operation(summary = "Get feedback by a specific interviewer for an interview")
    @ApiResponse(responseCode = "200", description = "Interviewer's feedback")
    @GetMapping("/{interviewId}/feedback/interviewer/{interviewerId}")
    @PreAuthorize("hasAuthority('VIEW_FEEDBACK') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<FeedbackResponse> getFeedbackByInterviewer(
            @PathVariable UUID interviewId,
            @PathVariable UUID interviewerId) {
        return ResponseEntity.ok(interviewService.getFeedbackByInterviewer(interviewId, interviewerId));
    }

    @Operation(summary = "Get all feedback submitted by an interviewer")
    @ApiResponse(responseCode = "200", description = "List of feedback by interviewer")
    @GetMapping("/feedback/interviewer/{interviewerId}")
    @PreAuthorize("hasAuthority('VIEW_FEEDBACK') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<FeedbackResponse>> getAllFeedbackByInterviewer(
            @PathVariable UUID interviewerId) {
        return ResponseEntity.ok(interviewService.getAllFeedbackByInterviewer(interviewerId));
    }

    // ==================== Filters ====================

    @Operation(summary = "Filter interviews by status")
    @ApiResponse(responseCode = "200", description = "Filtered interview list")
    @GetMapping("/filter/status")
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByStatus(
            @RequestParam InterviewStatus status) {
        return ResponseEntity.ok(interviewService.getInterviewsByStatus(status));
    }

    @Operation(summary = "Filter interviews by status (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated filtered interviews")
    @GetMapping("/filter/status/paginated")
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<InterviewResponse>> getInterviewsByStatusPaginated(
            @RequestParam InterviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(interviewService.getInterviewsByStatusPaginated(status, page, size));
    }

    @Operation(summary = "Filter interviews by date range")
    @ApiResponse(responseCode = "200", description = "Filtered interview list")
    @GetMapping("/filter/date-range")
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByDateRange(
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ResponseEntity.ok(interviewService.getInterviewsByDateRange(from, to));
    }

    @Operation(summary = "Filter interviews by date range (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated filtered interviews")
    @GetMapping("/filter/date-range/paginated")
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<InterviewResponse>> getInterviewsByDateRangePaginated(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(interviewService.getInterviewsByDateRangePaginated(from, to, page, size));
    }
}
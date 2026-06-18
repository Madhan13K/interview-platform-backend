package com.interview_platform_backend.interview_platform_backend.candidatefeedback.controller;

import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.CandidateFeedbackResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.FeedbackSummaryResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.SubmitCandidateFeedbackRequest;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.service.CandidateFeedbackService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidate-feedback")
@Tag(name = "Candidate Feedback", description = "Candidate reverse feedback endpoints")
public class CandidateFeedbackController {

    private final CandidateFeedbackService candidateFeedbackService;
    private final SecurityHelper securityHelper;

    public CandidateFeedbackController(CandidateFeedbackService candidateFeedbackService,
                                       SecurityHelper securityHelper) {
        this.candidateFeedbackService = candidateFeedbackService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Submit candidate feedback for an interview")
    @ApiResponse(responseCode = "200", description = "Feedback submitted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "409", description = "Feedback already submitted")
    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateFeedbackResponse> submitFeedback(
            @RequestBody @Valid SubmitCandidateFeedbackRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(candidateFeedbackService.submitFeedback(request, currentUserId));
    }

    @Operation(summary = "Get feedback for a specific interview")
    @ApiResponse(responseCode = "200", description = "Feedback list retrieved")
    @ApiResponse(responseCode = "404", description = "Interview not found")
    @GetMapping("/interview/{interviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('CANDIDATE')")
    public ResponseEntity<List<CandidateFeedbackResponse>> getFeedbackByInterview(
            @PathVariable UUID interviewId) {
        return ResponseEntity.ok(candidateFeedbackService.getFeedbackByInterview(interviewId));
    }

    @Operation(summary = "Get aggregate feedback summary")
    @ApiResponse(responseCode = "200", description = "Feedback summary retrieved")
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<FeedbackSummaryResponse> getFeedbackSummary() {
        return ResponseEntity.ok(candidateFeedbackService.getFeedbackSummary());
    }

    @Operation(summary = "Get my submitted feedback")
    @ApiResponse(responseCode = "200", description = "My feedback retrieved")
    @GetMapping("/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<PaginatedResponse<CandidateFeedbackResponse>> getMyFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(candidateFeedbackService.getMyFeedback(currentUserId, page, size));
    }
}

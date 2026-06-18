package com.interview_platform_backend.interview_platform_backend.scorecard.controller;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.scorecard.dto.*;
import com.interview_platform_backend.interview_platform_backend.scorecard.service.EvaluationScorecardService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scorecards")
public class EvaluationScorecardController {

    private final EvaluationScorecardService scorecardService;
    private final SecurityHelper securityHelper;

    public EvaluationScorecardController(EvaluationScorecardService scorecardService,
                                          SecurityHelper securityHelper) {
        this.scorecardService = scorecardService;
        this.securityHelper = securityHelper;
    }

    // ==================== Criteria Endpoints ====================

    @PostMapping("/criteria")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<CriteriaResponse> createCriteria(@Valid @RequestBody CreateCriteriaRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(scorecardService.createCriteria(request, userId));
    }

    @GetMapping("/criteria")
    public ResponseEntity<List<CriteriaResponse>> getAllCriteria() {
        return ResponseEntity.ok(scorecardService.getAllCriteria());
    }

    @GetMapping("/criteria/type/{type}")
    public ResponseEntity<List<CriteriaResponse>> getCriteriaByType(@PathVariable InterviewType type) {
        return ResponseEntity.ok(scorecardService.getCriteriaByType(type));
    }

    @GetMapping("/criteria/{criteriaId}")
    public ResponseEntity<CriteriaResponse> getCriteriaById(@PathVariable UUID criteriaId) {
        return ResponseEntity.ok(scorecardService.getCriteriaById(criteriaId));
    }

    @PutMapping("/criteria/{criteriaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<CriteriaResponse> updateCriteria(
            @PathVariable UUID criteriaId,
            @Valid @RequestBody CreateCriteriaRequest request) {
        return ResponseEntity.ok(scorecardService.updateCriteria(criteriaId, request));
    }

    @DeleteMapping("/criteria/{criteriaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<Void> deleteCriteria(@PathVariable UUID criteriaId) {
        scorecardService.deleteCriteria(criteriaId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Scorecard Submission ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<ScorecardResponse> submitScorecard(@Valid @RequestBody SubmitScorecardRequest request) {
        UUID interviewerId = securityHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(scorecardService.submitScorecard(request, interviewerId));
    }

    // ==================== Scorecard Retrieval ====================

    @GetMapping("/{scorecardId}")
    public ResponseEntity<ScorecardResponse> getScorecard(@PathVariable UUID scorecardId) {
        return ResponseEntity.ok(scorecardService.getScorecard(scorecardId));
    }

    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<List<ScorecardResponse>> getScorecardsByInterview(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(scorecardService.getScorecardsByInterview(interviewId));
    }

    @GetMapping("/interview/{interviewId}/interviewer/{interviewerId}")
    public ResponseEntity<ScorecardResponse> getScorecardByInterviewAndInterviewer(
            @PathVariable UUID interviewId, @PathVariable UUID interviewerId) {
        return ResponseEntity.ok(scorecardService.getScorecardByInterviewAndInterviewer(interviewId, interviewerId));
    }

    @GetMapping("/interviewer/{interviewerId}")
    public ResponseEntity<List<ScorecardResponse>> getScorecardsByInterviewer(@PathVariable UUID interviewerId) {
        return ResponseEntity.ok(scorecardService.getScorecardsByInterviewer(interviewerId));
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<ScorecardResponse>> getScorecardsByCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(scorecardService.getScorecardsByCandidate(candidateId));
    }

    // ==================== Summary / Analytics ====================

    @GetMapping("/interview/{interviewId}/summary")
    public ResponseEntity<CandidateScorecardSummary> getCandidateSummary(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(scorecardService.getCandidateSummary(interviewId));
    }
}


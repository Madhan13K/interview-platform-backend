package com.interview_platform_backend.interview_platform_backend.pipeline.controller;

import com.interview_platform_backend.interview_platform_backend.pipeline.dto.*;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.CandidatePipelineStatus;
import com.interview_platform_backend.interview_platform_backend.pipeline.service.PipelineService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelineController {

    private final PipelineService pipelineService;
    private final SecurityHelper securityHelper;

    public PipelineController(PipelineService pipelineService, SecurityHelper securityHelper) {
        this.pipelineService = pipelineService;
        this.securityHelper = securityHelper;
    }

    // ==================== Pipeline CRUD ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<PipelineResponse> createPipeline(@Valid @RequestBody CreatePipelineRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.createPipeline(request, userId));
    }

    @GetMapping("/{pipelineId}")
    public ResponseEntity<PipelineResponse> getPipeline(@PathVariable UUID pipelineId) {
        return ResponseEntity.ok(pipelineService.getPipeline(pipelineId));
    }

    @GetMapping
    public ResponseEntity<List<PipelineResponse>> getAllPipelines() {
        return ResponseEntity.ok(pipelineService.getAllPipelines());
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<PipelineResponse>> getPipelinesByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(pipelineService.getPipelinesByDepartment(department));
    }

    @PutMapping("/{pipelineId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<PipelineResponse> updatePipeline(
            @PathVariable UUID pipelineId,
            @Valid @RequestBody UpdatePipelineRequest request) {
        return ResponseEntity.ok(pipelineService.updatePipeline(pipelineId, request));
    }

    @DeleteMapping("/{pipelineId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<Void> deletePipeline(@PathVariable UUID pipelineId) {
        pipelineService.deletePipeline(pipelineId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Candidate Pipeline ====================

    @PostMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<CandidatePipelineResponse> addCandidateToPipeline(
            @Valid @RequestBody AddCandidateToPipelineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.addCandidateToPipeline(request));
    }

    @GetMapping("/candidates/{candidatePipelineId}")
    public ResponseEntity<CandidatePipelineResponse> getCandidatePipeline(@PathVariable UUID candidatePipelineId) {
        return ResponseEntity.ok(pipelineService.getCandidatePipeline(candidatePipelineId));
    }

    @GetMapping("/{pipelineId}/candidates")
    public ResponseEntity<List<CandidatePipelineResponse>> getCandidatesInPipeline(@PathVariable UUID pipelineId) {
        return ResponseEntity.ok(pipelineService.getCandidatesInPipeline(pipelineId));
    }

    @GetMapping("/candidates/user/{candidateId}")
    public ResponseEntity<List<CandidatePipelineResponse>> getCandidatePipelines(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(pipelineService.getCandidatePipelines(candidateId));
    }

    // ==================== Stage Progression ====================

    @PostMapping("/candidates/{candidatePipelineId}/advance")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<CandidatePipelineResponse> advanceToNextStage(
            @PathVariable UUID candidatePipelineId,
            @RequestParam(required = false) String feedback) {
        return ResponseEntity.ok(pipelineService.advanceToNextStage(candidatePipelineId, feedback));
    }

    @PostMapping("/candidates/{candidatePipelineId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<CandidatePipelineResponse> rejectCandidate(
            @PathVariable UUID candidatePipelineId,
            @RequestParam(required = false) String feedback) {
        return ResponseEntity.ok(pipelineService.rejectCandidate(candidatePipelineId, feedback));
    }

    @PatchMapping("/candidates/{candidatePipelineId}/stages/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<CandidatePipelineResponse> updateStageProgress(
            @PathVariable UUID candidatePipelineId,
            @PathVariable UUID stageId,
            @RequestBody @Valid UpdateStageProgressRequest request) {
        return ResponseEntity.ok(pipelineService.updateStageProgress(candidatePipelineId, stageId, request));
    }

    @PatchMapping("/candidates/{candidatePipelineId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<CandidatePipelineResponse> updateStatus(
            @PathVariable UUID candidatePipelineId,
            @RequestParam CandidatePipelineStatus status) {
        return ResponseEntity.ok(pipelineService.updateCandidatePipelineStatus(candidatePipelineId, status));
    }
}


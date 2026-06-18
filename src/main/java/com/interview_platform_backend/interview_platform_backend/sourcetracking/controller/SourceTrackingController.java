package com.interview_platform_backend.interview_platform_backend.sourcetracking.controller;

import com.interview_platform_backend.interview_platform_backend.sourcetracking.dto.SourceDashboardResponse;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.dto.SourceEffectivenessResponse;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.dto.TrackSourceRequest;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.CandidateSource;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.SourceType;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.service.SourceTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
@Tag(name = "Source Effectiveness", description = "Track candidate sources and calculate ROI")
public class SourceTrackingController {

    private final SourceTrackingService sourceTrackingService;

    public SourceTrackingController(SourceTrackingService sourceTrackingService) {
        this.sourceTrackingService = sourceTrackingService;
    }

    @Operation(summary = "Track a candidate source for an application")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<Void> trackSource(@RequestBody @Valid TrackSourceRequest request) {
        sourceTrackingService.trackSource(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get source effectiveness metrics aggregated by source type")
    @GetMapping("/effectiveness")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<SourceEffectivenessResponse>> getSourceEffectiveness() {
        return ResponseEntity.ok(sourceTrackingService.getSourceEffectiveness());
    }

    @Operation(summary = "Get full source tracking dashboard")
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<SourceDashboardResponse> getSourceDashboard() {
        return ResponseEntity.ok(sourceTrackingService.getSourceDashboard());
    }

    @Operation(summary = "Get ROI breakdown by specific source")
    @GetMapping("/roi")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<SourceEffectivenessResponse> getSourceROI(@RequestParam SourceType source) {
        return ResponseEntity.ok(sourceTrackingService.getSourceROI(source));
    }

    @Operation(summary = "Get top performing sources")
    @GetMapping("/top")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<SourceEffectivenessResponse>> getTopSources(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(sourceTrackingService.getTopSources(limit));
    }
}

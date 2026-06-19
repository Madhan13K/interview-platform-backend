package com.interview_platform_backend.interview_platform_backend.dei.controller;

import com.interview_platform_backend.interview_platform_backend.dei.dto.*;
import com.interview_platform_backend.interview_platform_backend.dei.service.DeiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dei")
@Tag(name = "DEI / Diversity Analytics", description = "Opt-in demographic tracking and diversity funnel analysis")
public class DeiController {

    private final DeiService deiService;

    public DeiController(DeiService deiService) {
        this.deiService = deiService;
    }

    @Operation(summary = "Submit or update demographic profile (opt-in, requires consent)")
    @PostMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<DemographicProfileResponse> submitDemographics(
            @RequestBody @Valid DemographicProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(deiService.submitDemographics(request, email));
    }

    @Operation(summary = "Get my demographic profile")
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<DemographicProfileResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(deiService.getMyDemographics(email));
    }

    @Operation(summary = "Revoke demographic data (delete profile)")
    @DeleteMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Void> revokeProfile(Authentication authentication) {
        String email = authentication.getName();
        deiService.revokeDemographics(email);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get diversity dashboard (aggregated statistics only - never individual data)")
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DiversityDashboardResponse> getDiversityDashboard() {
        return ResponseEntity.ok(deiService.getDiversityDashboard());
    }

    @Operation(summary = "Get diversity funnel analysis by pipeline stage (aggregated)")
    @GetMapping("/funnel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DiversityFunnelResponse>> getFunnelAnalysis() {
        return ResponseEntity.ok(deiService.getFunnelAnalysis());
    }
}

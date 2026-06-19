package com.interview_platform_backend.interview_platform_backend.referral.controller;

import com.interview_platform_backend.interview_platform_backend.referral.dto.CreateReferralRequest;
import com.interview_platform_backend.interview_platform_backend.referral.dto.ReferralResponse;
import com.interview_platform_backend.interview_platform_backend.referral.dto.ReferralStatsResponse;
import com.interview_platform_backend.interview_platform_backend.referral.entity.ReferralStatus;
import com.interview_platform_backend.interview_platform_backend.referral.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@Tag(name = "Referral Program", description = "Employee referral tracking with bonus workflows")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @Operation(summary = "Create a new referral")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReferralResponse> createReferral(
            @RequestBody @Valid CreateReferralRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(referralService.createReferral(request, email));
    }

    @Operation(summary = "Get my referrals")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReferralResponse>> getMyReferrals(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(referralService.getMyReferrals(email));
    }

    @Operation(summary = "Get referral by code")
    @GetMapping("/code/{referralCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReferralResponse> getReferralByCode(@PathVariable String referralCode) {
        return ResponseEntity.ok(referralService.getReferralByCode(referralCode));
    }

    @Operation(summary = "Update referral status")
    @PatchMapping("/{referralId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<ReferralResponse> updateStatus(
            @PathVariable UUID referralId,
            @RequestParam ReferralStatus status) {
        return ResponseEntity.ok(referralService.updateStatus(referralId, status));
    }

    @Operation(summary = "Get my referral statistics")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReferralStatsResponse> getReferralStats(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(referralService.getReferralStats(email));
    }

    @Operation(summary = "Get referral leaderboard")
    @GetMapping("/leaderboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        return ResponseEntity.ok(referralService.getLeaderboard());
    }

    @Operation(summary = "Mark referral bonus as paid")
    @PatchMapping("/{referralId}/bonus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferralResponse> markBonusPaid(
            @PathVariable UUID referralId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(referralService.markBonusPaid(referralId, amount));
    }
}

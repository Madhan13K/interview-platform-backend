package com.interview_platform_backend.interview_platform_backend.offer.controller;

import com.interview_platform_backend.interview_platform_backend.offer.dto.*;
import com.interview_platform_backend.interview_platform_backend.offer.service.OfferLetterService;
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
@RequestMapping("/api/v1/offers")
@Tag(name = "Offer Letters", description = "Offer letter management with approval workflow and e-signature")
public class OfferLetterController {

    private final OfferLetterService offerLetterService;

    public OfferLetterController(OfferLetterService offerLetterService) {
        this.offerLetterService = offerLetterService;
    }

    @Operation(summary = "Create a new offer letter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer letter created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate offer")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<OfferLetterResponse> createOffer(
            @RequestBody @Valid CreateOfferRequest request,
            Authentication authentication) {
        String creatorEmail = authentication.getName();
        return ResponseEntity.ok(offerLetterService.createOffer(request, creatorEmail));
    }

    @Operation(summary = "Get offer letter by ID")
    @ApiResponse(responseCode = "200", description = "Offer letter found")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OfferLetterResponse> getOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(offerLetterService.getOffer(id));
    }

    @Operation(summary = "Submit offer for approval workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer submitted for approval"),
            @ApiResponse(responseCode = "400", description = "Offer not in DRAFT status")
    })
    @PostMapping("/{id}/submit-approval")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<OfferLetterResponse> submitForApproval(@PathVariable UUID id) {
        return ResponseEntity.ok(offerLetterService.submitForApproval(id));
    }

    @Operation(summary = "Process approval decision (approve/reject)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval decision processed"),
            @ApiResponse(responseCode = "400", description = "Not pending approval or not approver's turn")
    })
    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OfferLetterResponse> processApproval(
            @PathVariable UUID id,
            @RequestBody @Valid ApprovalDecisionRequest decision,
            Authentication authentication) {
        String approverEmail = authentication.getName();
        return ResponseEntity.ok(offerLetterService.processApproval(id, approverEmail, decision));
    }

    @Operation(summary = "Send approved offer to candidate")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer sent to candidate"),
            @ApiResponse(responseCode = "400", description = "Offer not in APPROVED status")
    })
    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<OfferLetterResponse> sendOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(offerLetterService.sendOffer(id));
    }

    @Operation(summary = "Mark offer as viewed by candidate")
    @ApiResponse(responseCode = "200", description = "Offer marked as viewed")
    @PostMapping("/{id}/view")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<OfferLetterResponse> viewOffer(
            @PathVariable UUID id,
            Authentication authentication) {
        String candidateEmail = authentication.getName();
        return ResponseEntity.ok(offerLetterService.viewOffer(id, candidateEmail));
    }

    @Operation(summary = "Accept or decline an offer (candidate)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Response recorded"),
            @ApiResponse(responseCode = "400", description = "Offer not in valid status or expired")
    })
    @PostMapping("/{id}/respond")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<OfferLetterResponse> respondToOffer(
            @PathVariable UUID id,
            @RequestParam boolean accepted,
            @RequestParam(required = false) String responseNotes,
            Authentication authentication) {
        String candidateEmail = authentication.getName();
        return ResponseEntity.ok(offerLetterService.respondToOffer(id, candidateEmail, accepted, responseNotes));
    }

    @Operation(summary = "Revoke an offer letter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer revoked"),
            @ApiResponse(responseCode = "400", description = "Cannot revoke accepted offer")
    })
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<OfferLetterResponse> revokeOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(offerLetterService.revokeOffer(id));
    }

    @Operation(summary = "Get my offers (candidate view)")
    @ApiResponse(responseCode = "200", description = "List of candidate's offers")
    @GetMapping("/candidate/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<List<OfferLetterResponse>> getMyOffers(Authentication authentication) {
        String candidateEmail = authentication.getName();
        return ResponseEntity.ok(offerLetterService.getOffersForCandidate(candidateEmail));
    }

    @Operation(summary = "Get offers for a specific job position")
    @ApiResponse(responseCode = "200", description = "List of offers for position")
    @GetMapping("/position/{positionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<OfferLetterResponse>> getOffersForPosition(@PathVariable UUID positionId) {
        return ResponseEntity.ok(offerLetterService.getOffersForPosition(positionId));
    }

    @Operation(summary = "Check e-signature status from provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "E-signature status retrieved"),
            @ApiResponse(responseCode = "400", description = "No e-signature configured")
    })
    @GetMapping("/{id}/esignature-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<OfferLetterResponse> checkESignatureStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(offerLetterService.checkESignatureStatus(id));
    }
}

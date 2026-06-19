package com.interview_platform_backend.interview_platform_backend.approval.controller;

import com.interview_platform_backend.interview_platform_backend.approval.dto.*;
import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalEntityType;
import com.interview_platform_backend.interview_platform_backend.approval.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Approval Workflows", description = "Generic configurable approval chains for offers, requisitions, and job postings")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Operation(summary = "Create an approval chain")
    @PostMapping("/chains")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<ApprovalChainResponse> createChain(
            @RequestBody @Valid CreateApprovalChainRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalService.createChain(request, email));
    }

    @Operation(summary = "Get all approval chains")
    @GetMapping("/chains")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<ApprovalChainResponse>> getChains(
            @RequestParam(required = false) ApprovalEntityType entityType) {
        return ResponseEntity.ok(approvalService.getChains(entityType));
    }

    @Operation(summary = "Submit an entity for approval")
    @PostMapping("/requests")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('HIRING_MANAGER')")
    public ResponseEntity<ApprovalRequestResponse> submitForApproval(
            @RequestParam ApprovalEntityType entityType,
            @RequestParam UUID entityId,
            @RequestParam UUID chainId,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalService.submitForApproval(entityType, entityId, chainId, email));
    }

    @Operation(summary = "Process an approval decision")
    @PostMapping("/requests/{requestId}/decisions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApprovalRequestResponse> processDecision(
            @PathVariable UUID requestId,
            @RequestBody @Valid SubmitDecisionRequest decisionRequest,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(approvalService.processDecision(requestId, decisionRequest, email));
    }

    @Operation(summary = "Get approval request status")
    @GetMapping("/requests/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApprovalRequestResponse> getRequestStatus(@PathVariable UUID requestId) {
        return ResponseEntity.ok(approvalService.getRequestStatus(requestId));
    }

    @Operation(summary = "Cancel an approval request")
    @DeleteMapping("/requests/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {
        String email = authentication.getName();
        approvalService.cancelRequest(requestId, email);
        return ResponseEntity.noContent().build();
    }
}

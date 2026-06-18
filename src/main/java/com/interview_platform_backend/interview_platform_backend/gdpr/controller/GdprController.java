package com.interview_platform_backend.interview_platform_backend.gdpr.controller;

import com.interview_platform_backend.interview_platform_backend.gdpr.dto.*;
import com.interview_platform_backend.interview_platform_backend.gdpr.entity.DataErasureRequest;
import com.interview_platform_backend.interview_platform_backend.gdpr.service.GdprService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gdpr")
public class GdprController {

    private final GdprService gdprService;
    private final SecurityHelper securityHelper;

    public GdprController(GdprService gdprService, SecurityHelper securityHelper) {
        this.gdprService = gdprService;
        this.securityHelper = securityHelper;
    }

    @PostMapping("/consent")
    public ResponseEntity<ConsentResponse> recordConsent(@Valid @RequestBody ConsentRequest request,
                                                         HttpServletRequest httpRequest) {
        UUID userId = securityHelper.getCurrentUserId();
        String ipAddress = getClientIpAddress(httpRequest);
        ConsentResponse response = gdprService.recordConsent(userId, request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consent")
    public ResponseEntity<List<ConsentResponse>> getConsents() {
        UUID userId = securityHelper.getCurrentUserId();
        List<ConsentResponse> consents = gdprService.getConsents(userId);
        return ResponseEntity.ok(consents);
    }

    @DeleteMapping("/consent/{consentType}")
    public ResponseEntity<ConsentResponse> revokeConsent(@PathVariable String consentType) {
        UUID userId = securityHelper.getCurrentUserId();
        ConsentResponse response = gdprService.revokeConsent(userId, consentType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    public ResponseEntity<DataExportResponse> exportUserData() {
        UUID userId = securityHelper.getCurrentUserId();
        DataExportResponse response = gdprService.exportUserData(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/erasure")
    public ResponseEntity<ErasureRequestResponse> requestErasure() {
        UUID userId = securityHelper.getCurrentUserId();
        ErasureRequestResponse response = gdprService.requestErasure(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/erasure/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ErasureRequestResponse>> getErasureRequests(
            @RequestParam(required = false) String status) {
        DataErasureRequest.ErasureStatus erasureStatus = null;
        if (status != null && !status.isBlank()) {
            erasureStatus = DataErasureRequest.ErasureStatus.valueOf(status.toUpperCase());
        }
        List<ErasureRequestResponse> responses = gdprService.getErasureRequests(erasureStatus);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/erasure/{requestId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ErasureRequestResponse> processErasure(@PathVariable UUID requestId) {
        UUID adminUserId = securityHelper.getCurrentUserId();
        ErasureRequestResponse response = gdprService.processErasure(requestId, adminUserId);
        return ResponseEntity.ok(response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

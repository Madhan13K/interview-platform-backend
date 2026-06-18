package com.interview_platform_backend.interview_platform_backend.security.mfa.controller;

import com.interview_platform_backend.interview_platform_backend.security.mfa.dto.MfaSetupResponse;
import com.interview_platform_backend.interview_platform_backend.security.mfa.dto.MfaVerifyRequest;
import com.interview_platform_backend.interview_platform_backend.security.mfa.service.MfaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/mfa")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        MfaSetupResponse response = mfaService.setupMfa(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyAndEnable(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        mfaService.verifyAndEnable(userId, request.getCode());
        return ResponseEntity.ok(Map.of("message", "MFA enabled successfully", "mfaEnabled", true));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        boolean valid = mfaService.verifyCode(userId, request.getCode());
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @DeleteMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableMfa(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        mfaService.disableMfa(userId);
        return ResponseEntity.ok(Map.of("message", "MFA disabled successfully", "mfaEnabled", false));
    }

    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<String> codes = mfaService.regenerateBackupCodes(userId);
        return ResponseEntity.ok(Map.of("backupCodes", codes));
    }
}

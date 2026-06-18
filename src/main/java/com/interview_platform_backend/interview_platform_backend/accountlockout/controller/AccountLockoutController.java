package com.interview_platform_backend.interview_platform_backend.accountlockout.controller;

import com.interview_platform_backend.interview_platform_backend.accountlockout.dto.IpBlockRequest;
import com.interview_platform_backend.interview_platform_backend.accountlockout.dto.LockoutStatusResponse;
import com.interview_platform_backend.interview_platform_backend.accountlockout.dto.LoginAttemptResponse;
import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.AccountLockout;
import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.IpBlocklist;
import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.LoginAttempt;
import com.interview_platform_backend.interview_platform_backend.accountlockout.service.AccountLockoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security")
@Tag(name = "Account Security", description = "Account lockout, IP blocking, and login audit management")
public class AccountLockoutController {

    private final AccountLockoutService accountLockoutService;

    public AccountLockoutController(AccountLockoutService accountLockoutService) {
        this.accountLockoutService = accountLockoutService;
    }

    // --- Account Lockout Management ---

    @Operation(summary = "Get lockout status for a user account")
    @ApiResponse(responseCode = "200", description = "Lockout status returned")
    @GetMapping("/lockout/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LockoutStatusResponse> getLockoutStatus(@PathVariable String email) {
        AccountLockout lockout = accountLockoutService.getLockoutStatus(email);
        if (lockout == null) {
            return ResponseEntity.ok(LockoutStatusResponse.builder()
                    .email(email)
                    .failedAttempts(0)
                    .locked(false)
                    .build());
        }
        return ResponseEntity.ok(LockoutStatusResponse.builder()
                .email(lockout.getEmail())
                .failedAttempts(lockout.getFailedAttempts())
                .locked(lockout.getLocked())
                .lockedAt(lockout.getLockedAt())
                .lockExpiresAt(lockout.getLockExpiresAt())
                .lastFailedAt(lockout.getLastFailedAt())
                .build());
    }

    @Operation(summary = "Unlock a locked user account")
    @ApiResponse(responseCode = "204", description = "Account unlocked")
    @PostMapping("/lockout/{email}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unlockAccount(@PathVariable String email) {
        accountLockoutService.unlockAccount(email);
        return ResponseEntity.noContent().build();
    }

    // --- IP Blocking Management ---

    @Operation(summary = "Get all currently blocked IPs")
    @ApiResponse(responseCode = "200", description = "List of blocked IPs")
    @GetMapping("/blocked-ips")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<IpBlocklist>> getBlockedIps() {
        return ResponseEntity.ok(accountLockoutService.getBlockedIps());
    }

    @Operation(summary = "Manually block an IP address")
    @ApiResponse(responseCode = "204", description = "IP blocked")
    @PostMapping("/block-ip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockIp(@RequestBody @Valid IpBlockRequest request,
                                         Authentication authentication) {
        accountLockoutService.blockIp(
                request.getIpAddress(),
                request.getReason(),
                request.getDurationMinutes(),
                authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unblock an IP address")
    @ApiResponse(responseCode = "204", description = "IP unblocked")
    @PostMapping("/unblock-ip/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unblockIp(@PathVariable String ipAddress) {
        accountLockoutService.unblockIp(ipAddress);
        return ResponseEntity.noContent().build();
    }

    // --- Login Audit ---

    @Operation(summary = "Get recent login attempts for a user")
    @ApiResponse(responseCode = "200", description = "Login attempts returned")
    @GetMapping("/login-attempts/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoginAttemptResponse>> getLoginAttempts(@PathVariable String email) {
        List<LoginAttempt> attempts = accountLockoutService.getRecentAttempts(email);
        List<LoginAttemptResponse> responses = attempts.stream()
                .map(a -> LoginAttemptResponse.builder()
                        .id(a.getId())
                        .email(a.getEmail())
                        .ipAddress(a.getIpAddress())
                        .userAgent(a.getUserAgent())
                        .successful(a.isSuccessful())
                        .failureReason(a.getFailureReason())
                        .attemptedAt(a.getAttemptedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(responses);
    }
}

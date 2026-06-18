package com.interview_platform_backend.interview_platform_backend.security.auth.controller;

import com.interview_platform_backend.interview_platform_backend.security.auth.dto.AdminCreateUserRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.AuthResponse;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.ForgotPasswordRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.LoginRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.RegisterRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.ResetPasswordRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.service.AuthenticationService;
import com.interview_platform_backend.interview_platform_backend.security.auth.service.EmailVerificationService;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.RefreshTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, Login, Refresh, Logout")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthenticationService authenticationService,
                          EmailVerificationService emailVerificationService) {
        this.authenticationService = authenticationService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * PUBLIC: Self-registration as CANDIDATE only.
     * Frontend: Sign-up page for candidates.
     */
    @Operation(summary = "Register as candidate", description = "Public self-registration — creates user with CANDIDATE role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful, verification email sent"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(
                authenticationService.register(request)
        );
    }

    /**
     * PUBLIC: Self-registration as INTERVIEWER (also gets CANDIDATE role).
     */
    @Operation(summary = "Register as interviewer", description = "Public self-registration — creates user with INTERVIEWER + CANDIDATE roles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/register/interviewer")
    public ResponseEntity<AuthResponse> registerAsInterviewer(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(
                authenticationService.registerWithRole(request, "INTERVIEWER")
        );
    }

    /**
     * PROTECTED: Admin/Recruiter creates a user with specific role(s).
     * Frontend: Admin dashboard → "Create User" form with role selector.
     */
    @Operation(summary = "Admin create user", description = "Admin/Recruiter creates a user with specified roles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created"),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or RECRUITER role")
    })
    @PostMapping("/admin/create-user")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<AuthResponse> adminCreateUser(@RequestBody @Valid AdminCreateUserRequest request) {
        return ResponseEntity.ok(
                authenticationService.adminCreateUser(request)
        );
    }

    /**
     * PUBLIC: Login — same for all roles.
     * JWT will contain user's roles; frontend reads them to show correct UI.
     */
    @Operation(summary = "Login", description = "Authenticate with email/password and receive JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(
                authenticationService.login(request)
        );
    }

    @Operation(summary = "Request a password reset email")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reset email sent (if email exists)"),
            @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authenticationService.forgotPassword(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reset password using reset token")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authenticationService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New tokens issued"),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authenticationService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout", description = "Invalidate the refresh token")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authenticationService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verify email address using token sent during registration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully");
    }

    @Operation(summary = "Resend verification email")
    @ApiResponse(responseCode = "204", description = "Verification email resent")
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody @Valid ForgotPasswordRequest request) {
        emailVerificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.noContent().build();
    }
}

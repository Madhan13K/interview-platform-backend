package com.interview_platform_backend.interview_platform_backend.security.auth.controller;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.exception.UnauthorizedException;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.AuthResponse;
import com.interview_platform_backend.interview_platform_backend.security.auth.service.AuthenticationService;
import com.interview_platform_backend.interview_platform_backend.security.auth.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerWebMvcTest {

    private MockMvc mockMvc;
    private AuthenticationService authenticationService;
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        emailVerificationService = mock(EmailVerificationService.class);
        AuthController authController = new AuthController(authenticationService, emailVerificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== Registration ====================

    @Test
    void register_success_returnsOkWithTokens() throws Exception {
        given(authenticationService.register(any()))
                .willReturn(AuthResponse.builder()
                        .accessToken("access-jwt")
                        .refreshToken("refresh-jwt")
                        .build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "email": "john@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-jwt"));

        verify(authenticationService).register(any());
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        given(authenticationService.register(any()))
                .willThrow(new DuplicateResourceException("User", "email", "john@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "email": "john@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidBody_missingEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidBody_shortPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "email": "john@example.com",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerAsInterviewer_success_returnsOk() throws Exception {
        given(authenticationService.registerWithRole(any(), eq("INTERVIEWER")))
                .willReturn(AuthResponse.builder()
                        .accessToken("access-jwt")
                        .refreshToken("refresh-jwt")
                        .build());

        mockMvc.perform(post("/api/v1/auth/register/interviewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Jane",
                                  "lastName": "Int",
                                  "email": "jane@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"));
    }

    // ==================== Login ====================

    @Test
    void login_returnsOk() throws Exception {
        given(authenticationService.login(any()))
                .willReturn(AuthResponse.builder()
                        .accessToken("token")
                        .refreshToken("refresh")
                        .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        given(authenticationService.login(any()))
                .willThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unverifiedAccount_returnsUnauthorized() throws Exception {
        given(authenticationService.login(any()))
                .willThrow(new UnauthorizedException("Please verify your email before logging in"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please verify your email before logging in"));
    }

    @Test
    void login_suspendedAccount_returnsUnauthorized() throws Exception {
        given(authenticationService.login(any()))
                .willThrow(new UnauthorizedException("Your account has been suspended. Contact support."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidEmailFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ==================== Refresh Token ====================

    @Test
    void refresh_validToken_returnsNewTokens() throws Exception {
        given(authenticationService.refreshToken("valid-refresh-token"))
                .willReturn(AuthResponse.builder()
                        .accessToken("new-access-token")
                        .refreshToken("new-refresh-token")
                        .build());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_invalidToken_returnsUnauthorized() throws Exception {
        given(authenticationService.refreshToken("invalid-token"))
                .willThrow(new UnauthorizedException("Invalid refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "invalid-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void refresh_expiredToken_returnsUnauthorized() throws Exception {
        given(authenticationService.refreshToken("expired-token"))
                .willThrow(new UnauthorizedException("Refresh token is not usable"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "expired-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token is not usable"));
    }

    @Test
    void refresh_reuseDetection_returnsUnauthorized() throws Exception {
        given(authenticationService.refreshToken("reused-token"))
                .willThrow(new UnauthorizedException("Refresh token reuse detected — all sessions invalidated"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "reused-token"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_tokenNotFoundInDb_returnsNotFound() throws Exception {
        given(authenticationService.refreshToken("unknown-token"))
                .willThrow(new ResourceNotFoundException("Refresh token not found"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "unknown-token"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    // ==================== Logout ====================

    @Test
    void logout_success_returnsNoContent() throws Exception {
        doNothing().when(authenticationService).logout("valid-refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authenticationService).logout("valid-refresh-token");
    }

    @Test
    void logout_invalidToken_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Refresh token not found"))
                .when(authenticationService).logout("invalid-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "invalid-token"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    // ==================== Forgot/Reset Password ====================

    @Test
    void forgotPassword_returnsNoContent() throws Exception {
        doNothing().when(authenticationService).forgotPassword(anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authenticationService).forgotPassword("user@example.com");
    }

    @Test
    void forgotPassword_withInvalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_returnsNoContent() throws Exception {
        doNothing().when(authenticationService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "reset-token",
                                  "newPassword": "Password@123"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authenticationService).resetPassword("reset-token", "Password@123");
    }

    @Test
    void resetPassword_invalidToken_returnsBadRequest() throws Exception {
        doThrow(new BadRequestException("Invalid or expired reset token"))
                .when(authenticationService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "expired-token",
                                  "newPassword": "Password@123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }

    // ==================== Email Verification ====================

    @Test
    void verifyEmail_success_returnsOk() throws Exception {
        doNothing().when(emailVerificationService).verifyEmail("valid-token");

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "valid-token"))
                .andExpect(status().isOk());

        verify(emailVerificationService).verifyEmail("valid-token");
    }

    @Test
    void verifyEmail_invalidToken_returnsBadRequest() throws Exception {
        doThrow(new BadRequestException("Invalid verification token"))
                .when(emailVerificationService).verifyEmail("bad-token");

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification token"));
    }

    @Test
    void verifyEmail_expiredToken_returnsBadRequest() throws Exception {
        doThrow(new BadRequestException("Verification token has expired"))
                .when(emailVerificationService).verifyEmail("expired-token");

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "expired-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification token has expired"));
    }

    @Test
    void verifyEmail_alreadyUsedToken_returnsBadRequest() throws Exception {
        doThrow(new BadRequestException("Verification token has already been used"))
                .when(emailVerificationService).verifyEmail("used-token");

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "used-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerification_returnsNoContent() throws Exception {
        doNothing().when(emailVerificationService).resendVerificationEmail("user@example.com");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(emailVerificationService).resendVerificationEmail("user@example.com");
    }
}

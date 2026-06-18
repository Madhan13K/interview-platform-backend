package com.interview_platform_backend.interview_platform_backend.security.auth.service;

import com.interview_platform_backend.interview_platform_backend.security.auth.dto.AdminCreateUserRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.AuthResponse;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.LoginRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.RegisterRequest;

public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);
    AuthResponse registerWithRole(RegisterRequest request, String roleName);
    AuthResponse adminCreateUser(AdminCreateUserRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken);
    AuthResponse refreshToken(String refreshToken);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}

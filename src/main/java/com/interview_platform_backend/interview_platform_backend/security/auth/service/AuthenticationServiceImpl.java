package com.interview_platform_backend.interview_platform_backend.security.auth.service;

import com.interview_platform_backend.interview_platform_backend.accountlockout.service.AccountLockoutService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.exception.UnauthorizedException;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.AdminCreateUserRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.AuthResponse;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.LoginRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.RegisterRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.service.EmailVerificationService;
import com.interview_platform_backend.interview_platform_backend.security.token.RefreshToken;
import com.interview_platform_backend.interview_platform_backend.security.token.RefreshTokenService;
import com.interview_platform_backend.interview_platform_backend.security.jwt.CustomUserDetailsService;
import com.interview_platform_backend.interview_platform_backend.security.jwt.JwtService;
import com.interview_platform_backend.interview_platform_backend.user.entity.AuthProvider;
import com.interview_platform_backend.interview_platform_backend.user.entity.PasswordResetToken;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserProfile;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserRole;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.PasswordResetTokenRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.RoleRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final EmailNotificationService emailNotificationService;

    private final EmailVerificationService emailVerificationService;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final CustomUserDetailsService customUserDetailsService;

    private final AuthenticationManager authenticationManager;

    private final AccountLockoutService accountLockoutService;

    @Value("${app.auth.reset-password-base-url:http://localhost:5173/reset-password}")
    private String resetPasswordBaseUrl;

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     UserRoleRepository userRoleRepository,
                                     PasswordResetTokenRepository passwordResetTokenRepository,
                                     PasswordEncoder passwordEncoder,
                                     JwtService jwtService,
                                     RefreshTokenService refreshTokenService,
                                     CustomUserDetailsService customUserDetailsService,
                                     AuthenticationManager authenticationManager,
                                     EmailNotificationService emailNotificationService,
                                     EmailVerificationService emailVerificationService,
                                     AccountLockoutService accountLockoutService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.customUserDetailsService = customUserDetailsService;
        this.authenticationManager = authenticationManager;
        this.emailNotificationService = emailNotificationService;
        this.emailVerificationService = emailVerificationService;
        this.accountLockoutService = accountLockoutService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        Role candidateRole = roleRepository.findByName("CANDIDATE")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "CANDIDATE"));
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(Instant.now());

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);
        User savedUser = userRepository.save(user);
        UserRole userRole = new UserRole();
        userRole.setUser(savedUser);
        userRole.setRole(candidateRole);
        userRole.setAssignedAt(Instant.now());
        userRoleRepository.save(userRole);

        // Send verification email
        emailVerificationService.sendVerificationEmail(savedUser);

        String[] authorities = new String[]{"ROLE_" + candidateRole.getName()};

        String jwtToken = jwtService.generateToken(
                org.springframework.security.core.userdetails
                        .User
                        .builder()
                        .username(savedUser.getEmail())
                        .password(savedUser.getPassword())
                        .authorities(authorities)
                        .build()
        );
        String refreshToken = jwtService.generateRefreshToken(
                org.springframework.security.core.userdetails
                        .User
                        .builder()
                        .username(savedUser.getEmail())
                        .password(savedUser.getPassword())
                        .authorities(authorities)
                        .build()
        );
        refreshTokenService.create(savedUser, refreshToken);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(jwtToken);
        authResponse.setRefreshToken(refreshToken);
        return authResponse;
    }

    /**
     * Register a user with a specific role (e.g., INTERVIEWER).
     * Used when an admin/recruiter invites someone with a predefined role.
     */
    @Override
    @Transactional
    public AuthResponse registerWithRole(RegisterRequest request, String roleName) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(Instant.now());

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);
        User savedUser = userRepository.save(user);

        // Assign the specified role
        UserRole userRole = new UserRole();
        userRole.setUser(savedUser);
        userRole.setRole(role);
        userRole.setAssignedAt(Instant.now());
        userRoleRepository.save(userRole);

        // If role is INTERVIEWER, also assign CANDIDATE (interviewers can also be candidates)
        if ("INTERVIEWER".equalsIgnoreCase(roleName)) {
            roleRepository.findByName("CANDIDATE").ifPresent(candidateRole -> {
                UserRole candidateUserRole = new UserRole();
                candidateUserRole.setUser(savedUser);
                candidateUserRole.setRole(candidateRole);
                candidateUserRole.setAssignedAt(Instant.now());
                userRoleRepository.save(candidateUserRole);
            });
        }

        // Send verification email
        emailVerificationService.sendVerificationEmail(savedUser);

        List<String> roleAuthorities = userRoleRepository.findByUser(savedUser)
                .stream()
                .map(ur -> "ROLE_" + ur.getRole().getName())
                .distinct()
                .toList();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPassword())
                .authorities(roleAuthorities.toArray(new String[0]))
                .build();

        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        refreshTokenService.create(savedUser, refreshToken);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(jwtToken);
        authResponse.setRefreshToken(refreshToken);
        return authResponse;
    }

    /**
     * Admin creates a user with multiple roles.
     * Only accessible to ADMIN or RECRUITER via @PreAuthorize.
     */
    @Override
    @Transactional
    public AuthResponse adminCreateUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);
        User savedUser = userRepository.save(user);

        // Assign all requested roles
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
            UserRole userRole = new UserRole();
            userRole.setUser(savedUser);
            userRole.setRole(role);
            userRole.setAssignedAt(Instant.now());
            userRoleRepository.save(userRole);
        }

        List<String> roleAuthorities = userRoleRepository.findByUser(savedUser)
                .stream()
                .map(ur -> "ROLE_" + ur.getRole().getName())
                .distinct()
                .toList();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPassword())
                .authorities(roleAuthorities.toArray(new String[0]))
                .build();

        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        refreshTokenService.create(savedUser, refreshToken);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(jwtToken);
        authResponse.setRefreshToken(refreshToken);
        return authResponse;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();
        String ipAddress = extractClientIp();
        String userAgent = extractUserAgent();

        // PRE-LOGIN CHECK: Account lockout and IP blocking
        String blockReason = accountLockoutService.checkPreLogin(email, ipAddress);
        if (blockReason != null) {
            // Record the blocked attempt
            accountLockoutService.recordFailedLogin(email, ipAddress, userAgent, blockReason);
            switch (blockReason) {
                case "IP_BLOCKED" -> throw new UnauthorizedException(
                        "Access denied. Your IP address has been temporarily blocked due to suspicious activity.");
                case "ACCOUNT_LOCKED" -> throw new UnauthorizedException(
                        "Account is locked due to too many failed login attempts. Please try again later or contact support.");
                default -> throw new UnauthorizedException("Access denied.");
            }
        }

        // AUTHENTICATE
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (BadCredentialsException e) {
            // Record the failed attempt — may trigger lockout
            accountLockoutService.recordFailedLogin(email, ipAddress, userAgent, "INVALID_CREDENTIALS");
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", email));

        // Block login for unverified or inactive users
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            accountLockoutService.recordFailedLogin(email, ipAddress, userAgent, "PENDING_VERIFICATION");
            throw new UnauthorizedException("Please verify your email before logging in");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            accountLockoutService.recordFailedLogin(email, ipAddress, userAgent, "ACCOUNT_SUSPENDED");
            throw new UnauthorizedException("Your account has been suspended. Contact support.");
        }
        if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.DELETED) {
            accountLockoutService.recordFailedLogin(email, ipAddress, userAgent, "ACCOUNT_INACTIVE");
            throw new UnauthorizedException("Your account is no longer active");
        }

        // SUCCESS — record successful login and reset counters
        accountLockoutService.recordSuccessfulLogin(email, ipAddress, userAgent);

        List<String> roleAuthorities = userRoleRepository.findByUser(user)
                .stream()
                .filter(userRole -> userRole.getRole() != null
                        && userRole.getRole().getName() != null)
                .map(userRole -> "ROLE_" + userRole.getRole().getName())
                .distinct()
                .toList();

        if (roleAuthorities.isEmpty()) {
            roleAuthorities = List.of("ROLE_USER");
        }

        String jwtToken = jwtService.generateToken(
                org.springframework.security.core.userdetails
                        .User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .authorities(roleAuthorities.toArray(new String[0]))
                        .build()
        );
        String refreshToken = jwtService.generateRefreshToken(
                org.springframework.security.core.userdetails
                        .User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .authorities(roleAuthorities.toArray(new String[0]))
                        .build()
        );
        refreshTokenService.create(user, refreshToken);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(jwtToken);
        authResponse.setRefreshToken(refreshToken);
        return authResponse;
    }

    /**
     * Extract client IP address from the current HTTP request.
     * Handles X-Forwarded-For header for proxied requests.
     */
    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isBlank()) {
                    return xRealIp.trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Fallback
        }
        return "unknown";
    }

    /**
     * Extract User-Agent from the current HTTP request.
     */
    private String extractUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ua = request.getHeader("User-Agent");
                return ua != null ? ua.substring(0, Math.min(ua.length(), 500)) : "unknown";
            }
        } catch (Exception e) {
            // Fallback
        }
        return "unknown";
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
        refreshTokenService.revokeToken(token);
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (!jwtService.isRefreshTokenValid(rawRefreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        RefreshToken refreshToken = refreshTokenService.findByToken(rawRefreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        // REPLAY DETECTION: if the token was already revoked, someone is reusing a stolen token.
        // Invalidate the entire token family to protect the user.
        if (refreshToken.isRevoked()) {
            refreshTokenService.revokeTokenFamily(refreshToken.getTokenFamily());
            throw new UnauthorizedException("Refresh token reuse detected — all sessions invalidated");
        }

        if (!refreshTokenService.isUsable(refreshToken)) {
            throw new UnauthorizedException("Refresh token is not usable");
        }

        String userName = jwtService.extractUsernameFromRefreshToken(rawRefreshToken);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);

        // Revoke the old refresh token (it's now single-use)
        refreshTokenService.revokeToken(refreshToken);

        // Generate new tokens
        String newJwtToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Create rotated token in the same family
        User user = userRepository.findByEmail(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userName));
        refreshTokenService.createRotated(user, newRefreshToken, refreshToken.getTokenFamily());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(newJwtToken);
        authResponse.setRefreshToken(newRefreshToken);
        return authResponse;
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getAuthProvider() != AuthProvider.LOCAL) {
                return;
            }

            invalidateResetTokensForUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .expiryTime(Instant.now().plus(Duration.ofHours(1)))
                    .used(false)
                    .user(user)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String resetLink = resetPasswordBaseUrl + "?token=" + token;
            String body = "We received a request to reset your password.\n\n"
                    + "Use this link to reset it:\n"
                    + resetLink
                    + "\n\n"
                    + "This link expires in 1 hour. If you did not request this, ignore this email.";

            emailNotificationService.sendEmail(user.getEmail(), "Reset your password", body);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed()) ||
                resetToken.getExpiryTime() == null ||
                resetToken.getExpiryTime().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        invalidateResetTokensForUser(user);
        refreshTokenService.revokeAllForUser(user);
    }

    private void invalidateResetTokensForUser(User user) {
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUser(user);
        for (PasswordResetToken token : tokens) {
            token.setUsed(true);
        }
        if (!tokens.isEmpty()) {
            passwordResetTokenRepository.saveAll(tokens);
        }
        passwordResetTokenRepository.deleteByExpiryTimeBefore(Instant.now());
    }
}
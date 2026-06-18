package com.interview_platform_backend.interview_platform_backend.security.auth.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.user.entity.EmailVerificationToken;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.EmailVerificationTokenRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    @Value("${app.auth.verification-base-url:http://localhost:5173/verify-email}")
    private String verificationBaseUrl;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    EmailNotificationService emailNotificationService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Generates a verification token and sends email to the user.
     * Called after registration.
     */
    public void sendVerificationEmail(User user) {
        // Invalidate any existing tokens
        invalidateTokensForUser(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .expiryTime(Instant.now().plus(Duration.ofMinutes(10)))
                .used(false)
                .user(user)
                .build();
        tokenRepository.save(verificationToken);

        String verificationLink = verificationBaseUrl + "?token=" + token;
        String body = "Welcome to Interview Platform!\n\n"
                + "Please verify your email address by clicking the link below:\n"
                + verificationLink
                + "\n\n"
                + "This link expires in 10 minutes.\n"
                + "If you did not create an account, please ignore this email.";

        emailNotificationService.sendEmail(user.getEmail(), "Verify your email address", body);
    }

    /**
     * Verifies the email using the token.
     * Sets user status from PENDING_VERIFICATION to ACTIVE.
     */
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (Boolean.TRUE.equals(verificationToken.getUsed())) {
            throw new BadRequestException("Verification token has already been used");
        }

        if (verificationToken.getExpiryTime().isBefore(Instant.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Email is already verified");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
    }

    /**
     * Resends verification email for users in PENDING_VERIFICATION state.
     */
    public void resendVerificationEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
                return; // silently ignore if already verified
            }
            sendVerificationEmail(user);
        });
    }

    private void invalidateTokensForUser(User user) {
        List<EmailVerificationToken> tokens = tokenRepository.findByUser(user);
        for (EmailVerificationToken t : tokens) {
            t.setUsed(true);
        }
        if (!tokens.isEmpty()) {
            tokenRepository.saveAll(tokens);
        }
        tokenRepository.deleteByExpiryTimeBefore(Instant.now());
    }
}




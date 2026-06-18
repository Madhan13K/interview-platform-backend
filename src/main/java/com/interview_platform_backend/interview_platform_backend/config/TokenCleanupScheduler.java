package com.interview_platform_backend.interview_platform_backend.config;

import com.interview_platform_backend.interview_platform_backend.user.repository.EmailVerificationTokenRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled task to clean up expired tokens from the database.
 * Runs every hour.
 */
@Component
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public TokenCleanupScheduler(PasswordResetTokenRepository passwordResetTokenRepository,
                                 EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @SchedulerLock(name = "cleanupExpiredTokens", lockAtLeastFor = "1m")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        passwordResetTokenRepository.deleteByExpiryTimeBefore(now);
        emailVerificationTokenRepository.deleteByExpiryTimeBefore(now);
        log.info("Cleaned up expired tokens at {}", now);
    }
}


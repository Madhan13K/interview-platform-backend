package com.interview_platform_backend.interview_platform_backend.accountlockout.service;

import com.interview_platform_backend.interview_platform_backend.accountlockout.config.AccountLockoutProperties;
import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.AccountLockout;
import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.IpBlocklist;
import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.LoginAttempt;
import com.interview_platform_backend.interview_platform_backend.accountlockout.repository.AccountLockoutRepository;
import com.interview_platform_backend.interview_platform_backend.accountlockout.repository.IpBlocklistRepository;
import com.interview_platform_backend.interview_platform_backend.accountlockout.repository.LoginAttemptRepository;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AccountLockoutService {

    private static final Logger log = LoggerFactory.getLogger(AccountLockoutService.class);

    private final AccountLockoutRepository accountLockoutRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final IpBlocklistRepository ipBlocklistRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final AccountLockoutProperties properties;

    public AccountLockoutService(AccountLockoutRepository accountLockoutRepository,
                                 LoginAttemptRepository loginAttemptRepository,
                                 IpBlocklistRepository ipBlocklistRepository,
                                 UserRepository userRepository,
                                 EmailNotificationService emailNotificationService,
                                 AccountLockoutProperties properties) {
        this.accountLockoutRepository = accountLockoutRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.ipBlocklistRepository = ipBlocklistRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
        this.properties = properties;
    }

    /**
     * Check if a login attempt should be blocked BEFORE authentication.
     * This is called before credentials are verified.
     *
     * @return null if allowed, or a reason string if blocked.
     */
    public String checkPreLogin(String email, String ipAddress) {
        if (!properties.isEnabled()) {
            return null;
        }

        // Check IP blocklist
        if (properties.isIpBlockingEnabled() && isIpBlocked(ipAddress)) {
            log.warn("Blocked login attempt from blocked IP: {} for email: {}", ipAddress, email);
            return "IP_BLOCKED";
        }

        // Check account lockout
        AccountLockout lockout = accountLockoutRepository.findByEmail(email).orElse(null);
        if (lockout != null && lockout.getLocked()) {
            // Check if lock has expired
            if (lockout.getLockExpiresAt() != null && lockout.getLockExpiresAt().isBefore(Instant.now())) {
                // Lock expired - unlock the account
                unlockAccount(email);
                return null;
            }
            log.warn("Login attempt on locked account: {} from IP: {}", email, ipAddress);
            return "ACCOUNT_LOCKED";
        }

        return null;
    }

    /**
     * Record a successful login attempt and reset failed counters.
     */
    public void recordSuccessfulLogin(String email, String ipAddress, String userAgent) {
        if (!properties.isEnabled()) {
            return;
        }

        // Record the attempt
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .successful(true)
                .build();
        loginAttemptRepository.save(attempt);

        // Reset failed attempt counter
        accountLockoutRepository.findByEmail(email).ifPresent(lockout -> {
            lockout.setFailedAttempts(0);
            lockout.setLocked(false);
            lockout.setLockedAt(null);
            lockout.setLockExpiresAt(null);
            accountLockoutRepository.save(lockout);
        });

        log.debug("Successful login recorded for: {} from IP: {}", email, ipAddress);
    }

    /**
     * Record a failed login attempt. May trigger lockout or IP block.
     */
    public void recordFailedLogin(String email, String ipAddress, String userAgent, String failureReason) {
        if (!properties.isEnabled()) {
            return;
        }

        // Record the attempt
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .successful(false)
                .failureReason(failureReason)
                .build();
        loginAttemptRepository.save(attempt);

        // Update account lockout counter
        AccountLockout lockout = accountLockoutRepository.findByEmail(email)
                .orElseGet(() -> {
                    AccountLockout newLockout = new AccountLockout();
                    newLockout.setEmail(email);
                    newLockout.setFailedAttempts(0);
                    newLockout.setLocked(false);
                    return newLockout;
                });

        lockout.setFailedAttempts(lockout.getFailedAttempts() + 1);
        lockout.setLastFailedAt(Instant.now());

        // Check if account should be locked
        if (lockout.getFailedAttempts() >= properties.getMaxFailedAttempts()) {
            lockout.setLocked(true);
            lockout.setLockedAt(Instant.now());

            if (properties.getLockDurationMinutes() > 0) {
                lockout.setLockExpiresAt(Instant.now().plus(
                        Duration.ofMinutes(properties.getLockDurationMinutes())));
            }
            // If lockDurationMinutes is 0, lock is permanent (no expiresAt)

            log.warn("Account locked for {} after {} failed attempts from IP: {}",
                    email, lockout.getFailedAttempts(), ipAddress);
        }

        accountLockoutRepository.save(lockout);

        // Check if we should send a security alert
        if (properties.isAlertsEnabled() && lockout.getFailedAttempts() >= properties.getAlertThreshold()) {
            sendSecurityAlert(email, ipAddress, lockout.getFailedAttempts());
        }

        // Check IP-based blocking
        if (properties.isIpBlockingEnabled()) {
            checkAndBlockIp(ipAddress);
        }

        log.debug("Failed login recorded for: {} from IP: {} (attempt #{})",
                email, ipAddress, lockout.getFailedAttempts());
    }

    /**
     * Check if an IP address is blocked.
     */
    public boolean isIpBlocked(String ipAddress) {
        return ipBlocklistRepository.findByIpAddressAndActiveTrue(ipAddress)
                .map(block -> {
                    // Check if block has expired
                    if (block.getExpiresAt() != null && block.getExpiresAt().isBefore(Instant.now())) {
                        block.setActive(false);
                        ipBlocklistRepository.save(block);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * Manually block an IP address (admin action).
     */
    public void blockIp(String ipAddress, String reason, Integer durationMinutes, String blockedBy) {
        IpBlocklist block = ipBlocklistRepository.findByIpAddressAndActiveTrue(ipAddress)
                .orElse(new IpBlocklist());

        block.setIpAddress(ipAddress);
        block.setReason(reason);
        block.setActive(true);
        block.setBlockedBy(blockedBy);

        if (durationMinutes != null && durationMinutes > 0) {
            block.setExpiresAt(Instant.now().plus(Duration.ofMinutes(durationMinutes)));
        } else {
            block.setExpiresAt(null); // Permanent
        }

        ipBlocklistRepository.save(block);
        log.info("IP {} blocked by {} for reason: {}", ipAddress, blockedBy, reason);
    }

    /**
     * Unblock an IP address.
     */
    public void unblockIp(String ipAddress) {
        ipBlocklistRepository.findByIpAddressAndActiveTrue(ipAddress).ifPresent(block -> {
            block.setActive(false);
            ipBlocklistRepository.save(block);
            log.info("IP {} unblocked", ipAddress);
        });
    }

    /**
     * Manually unlock a user account (admin action).
     */
    public void unlockAccount(String email) {
        accountLockoutRepository.findByEmail(email).ifPresent(lockout -> {
            lockout.setLocked(false);
            lockout.setLockedAt(null);
            lockout.setLockExpiresAt(null);
            lockout.setFailedAttempts(0);
            accountLockoutRepository.save(lockout);
            log.info("Account unlocked for: {}", email);
        });
    }

    /**
     * Get lockout status for an account.
     */
    @Transactional(readOnly = true)
    public AccountLockout getLockoutStatus(String email) {
        return accountLockoutRepository.findByEmail(email).orElse(null);
    }

    /**
     * Get all currently blocked IPs.
     */
    @Transactional(readOnly = true)
    public List<IpBlocklist> getBlockedIps() {
        return ipBlocklistRepository.findByActiveTrue();
    }

    /**
     * Get recent login attempts for a user.
     */
    @Transactional(readOnly = true)
    public List<LoginAttempt> getRecentAttempts(String email) {
        return loginAttemptRepository.findByEmailOrderByAttemptedAtDesc(email);
    }

    /**
     * Check if IP should be blocked based on failed attempts count.
     */
    private void checkAndBlockIp(String ipAddress) {
        Instant windowStart = Instant.now().minus(Duration.ofMinutes(properties.getAttemptWindowMinutes()));
        long failedFromIp = loginAttemptRepository.countFailedAttemptsFromIpSince(ipAddress, windowStart);

        if (failedFromIp >= properties.getMaxFailedAttemptsPerIp()) {
            if (!ipBlocklistRepository.existsByIpAddressAndActiveTrue(ipAddress)) {
                IpBlocklist block = IpBlocklist.builder()
                        .ipAddress(ipAddress)
                        .reason("BRUTE_FORCE")
                        .failedAttempts((int) failedFromIp)
                        .active(true)
                        .blockedBy("SYSTEM")
                        .expiresAt(Instant.now().plus(Duration.ofMinutes(properties.getIpBlockDurationMinutes())))
                        .build();
                ipBlocklistRepository.save(block);
                log.warn("IP {} auto-blocked after {} failed attempts", ipAddress, failedFromIp);
            }
        }
    }

    /**
     * Send a security alert email to the user about suspicious activity.
     */
    private void sendSecurityAlert(String email, String ipAddress, int failedAttempts) {
        try {
            userRepository.findByEmail(email).ifPresent(user -> {
                String subject = "Security Alert: Suspicious Login Activity";
                String body = String.format(
                        "We detected %d failed login attempts on your account from IP address %s.\n\n"
                                + "If this was not you, please:\n"
                                + "1. Change your password immediately\n"
                                + "2. Enable two-factor authentication\n"
                                + "3. Contact our support team\n\n"
                                + "If you made these attempts, your account may be temporarily locked "
                                + "after %d total failed attempts for security purposes.\n\n"
                                + "Account will auto-unlock after %d minutes.",
                        failedAttempts, ipAddress,
                        properties.getMaxFailedAttempts(),
                        properties.getLockDurationMinutes()
                );
                emailNotificationService.sendEmail(email, subject, body);
                log.info("Security alert sent to {} after {} failed attempts from IP {}",
                        email, failedAttempts, ipAddress);
            });
        } catch (Exception e) {
            log.error("Failed to send security alert to {}: {}", email, e.getMessage());
        }
    }

    /**
     * Scheduled cleanup: remove expired IP blocks and old login attempt records.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void cleanupExpiredBlocks() {
        // Deactivate expired IP blocks
        List<IpBlocklist> expired = ipBlocklistRepository.findExpiredBlocks(Instant.now());
        for (IpBlocklist block : expired) {
            block.setActive(false);
        }
        if (!expired.isEmpty()) {
            ipBlocklistRepository.saveAll(expired);
            log.info("Cleaned up {} expired IP blocks", expired.size());
        }

        // Clean up old login attempts (older than 30 days)
        Instant cutoff = Instant.now().minus(Duration.ofDays(30));
        loginAttemptRepository.deleteByAttemptedAtBefore(cutoff);
    }
}

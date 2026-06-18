package com.interview_platform_backend.interview_platform_backend.accountlockout.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security.lockout")
@Getter
@Setter
public class AccountLockoutProperties {

    /**
     * Maximum failed login attempts before account is locked.
     */
    private int maxFailedAttempts = 5;

    /**
     * Duration (in minutes) for which the account stays locked.
     * Set to 0 for permanent lock (until admin unlocks).
     */
    private int lockDurationMinutes = 30;

    /**
     * Time window (in minutes) for counting failed attempts.
     * Only attempts within this window count toward lockout.
     */
    private int attemptWindowMinutes = 15;

    /**
     * Maximum failed attempts from a single IP before IP is blocked.
     */
    private int maxFailedAttemptsPerIp = 20;

    /**
     * Duration (in minutes) for which an IP stays blocked.
     */
    private int ipBlockDurationMinutes = 60;

    /**
     * Whether to send alert notifications on suspicious login activity.
     */
    private boolean alertsEnabled = true;

    /**
     * Number of failed attempts that trigger a security alert email.
     */
    private int alertThreshold = 3;

    /**
     * Whether IP-based blocking is enabled.
     */
    private boolean ipBlockingEnabled = true;

    /**
     * Whether account lockout is enabled.
     */
    private boolean enabled = true;
}

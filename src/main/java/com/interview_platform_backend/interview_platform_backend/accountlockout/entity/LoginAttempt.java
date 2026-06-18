package com.interview_platform_backend.interview_platform_backend.accountlockout.entity;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Records each login attempt (successful or failed) for audit and lockout purposes.
 */
@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Email used in the login attempt (may not correspond to an existing user).
     */
    @Column(nullable = false)
    private String email;

    /**
     * IP address of the client making the attempt.
     */
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    /**
     * User-Agent header value.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Whether this attempt was successful.
     */
    @Column(nullable = false)
    private boolean successful;

    /**
     * Reason for failure (e.g., "INVALID_CREDENTIALS", "ACCOUNT_LOCKED", "ACCOUNT_SUSPENDED").
     */
    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    /**
     * Geographic location derived from IP (optional).
     */
    @Column(length = 200)
    private String location;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @PrePersist
    protected void onCreate() {
        attemptedAt = Instant.now();
    }
}

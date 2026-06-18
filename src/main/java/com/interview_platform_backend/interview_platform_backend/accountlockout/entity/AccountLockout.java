package com.interview_platform_backend.interview_platform_backend.accountlockout.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks account lockout status per user.
 * Separate from User entity to avoid overloading the user table.
 */
@Entity
@Table(name = "account_lockouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLockout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Current count of consecutive failed attempts.
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * Whether the account is currently locked.
     */
    @Column(name = "locked", nullable = false)
    @Builder.Default
    private Boolean locked = false;

    /**
     * When the account was locked.
     */
    @Column(name = "locked_at")
    private Instant lockedAt;

    /**
     * When the lockout expires (null = permanent until admin unlocks).
     */
    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    /**
     * Last failed attempt timestamp.
     */
    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

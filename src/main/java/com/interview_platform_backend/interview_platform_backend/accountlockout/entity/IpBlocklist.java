package com.interview_platform_backend.interview_platform_backend.accountlockout.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores blocked IP addresses. IPs are blocked after too many failed attempts
 * from the same address or manually by an admin.
 */
@Entity
@Table(name = "ip_blocklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpBlocklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ip_address", nullable = false, unique = true, length = 45)
    private String ipAddress;

    /**
     * Reason for blocking (e.g., "BRUTE_FORCE", "MANUAL_BLOCK", "SUSPICIOUS_ACTIVITY").
     */
    @Column(nullable = false, length = 100)
    private String reason;

    /**
     * When this block expires. Null means permanent.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Number of failed attempts that triggered this block.
     */
    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    /**
     * Who blocked this IP (admin email or "SYSTEM").
     */
    @Column(name = "blocked_by", length = 200)
    @Builder.Default
    private String blockedBy = "SYSTEM";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        blockedAt = Instant.now();
    }
}

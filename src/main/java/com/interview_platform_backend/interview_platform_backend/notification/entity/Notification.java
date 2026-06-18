package com.interview_platform_backend.interview_platform_backend.notification.entity;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String type; // INTERVIEW_SCHEDULED, INTERVIEW_CANCELLED, FEEDBACK_RECEIVED, etc.

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "reference_id")
    private UUID referenceId; // interview ID, feedback ID, etc.

    @Column(name = "reference_type", length = 50)
    private String referenceType; // INTERVIEW, FEEDBACK, USER

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (isRead == null) isRead = false;
    }
}


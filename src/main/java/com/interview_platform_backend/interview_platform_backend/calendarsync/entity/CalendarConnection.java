package com.interview_platform_backend.interview_platform_backend.calendarsync.entity;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_connections",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_calendar_connection_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarProvider provider;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private Instant tokenExpiresAt;

    @Column(nullable = false)
    @Builder.Default
    private String calendarId = "primary";

    @Column(nullable = false)
    @Builder.Default
    private boolean syncEnabled = true;

    private Instant lastSyncAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

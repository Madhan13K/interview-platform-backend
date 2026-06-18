package com.interview_platform_backend.interview_platform_backend.calendarsync.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_calendar_event_connection_interview",
                        columnNames = {"connection_id", "interview_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private CalendarConnection connection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(nullable = false)
    private String externalEventId;

    private String externalCalendarId;

    private Instant lastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncDirection syncDirection;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastSyncedAt = Instant.now();
    }
}

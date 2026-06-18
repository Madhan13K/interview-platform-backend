package com.interview_platform_backend.interview_platform_backend.meeting.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false, unique = true)
    private Interview interview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingProvider provider;

    @Column(name = "meeting_url", nullable = false, length = 1000)
    private String meetingUrl;

    @Column(name = "host_url", length = 1000)
    private String hostUrl;

    @Column(name = "meeting_id")
    private String meetingId;

    @Column(name = "passcode", length = 100)
    private String passcode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}


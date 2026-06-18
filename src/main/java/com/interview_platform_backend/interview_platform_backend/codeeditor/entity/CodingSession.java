package com.interview_platform_backend.interview_platform_backend.codeeditor.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coding_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(name = "code_content", columnDefinition = "TEXT")
    private String codeContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_edited_by")
    private User lastEditedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = Instant.now();
        if (language == null) language = "java";
    }
}


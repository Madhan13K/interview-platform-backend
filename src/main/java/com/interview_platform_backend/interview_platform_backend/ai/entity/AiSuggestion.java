package com.interview_platform_backend.interview_platform_backend.ai.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AiSuggestionType type;

    @Column(columnDefinition = "TEXT")
    private String inputContext;

    @Column(columnDefinition = "TEXT")
    private String outputContent;

    private String model;

    private Integer tokensUsed;

    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiSuggestionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum AiSuggestionType {
        QUESTION_SUGGESTION,
        RESUME_PARSE,
        INTERVIEW_SUMMARY,
        CANDIDATE_ASSESSMENT
    }

    public enum AiSuggestionStatus {
        GENERATED,
        ACCEPTED,
        REJECTED
    }
}

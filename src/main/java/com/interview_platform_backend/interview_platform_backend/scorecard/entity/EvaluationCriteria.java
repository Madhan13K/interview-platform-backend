package com.interview_platform_backend.interview_platform_backend.scorecard.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Defines a single evaluation criterion (e.g., "Problem Solving", "Communication").
 * Criteria can be global or scoped to a specific interview type.
 */
@Entity
@Table(name = "evaluation_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", length = 30)
    private InterviewType interviewType;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Integer maxScore = 5;

    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Double weight = 1.0;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}


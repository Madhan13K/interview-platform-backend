package com.interview_platform_backend.interview_platform_backend.scorecard.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A scorecard submitted by an interviewer for a specific interview.
 * Contains multiple scored entries (one per criteria) plus an overall summary.
 */
@Entity
@Table(name = "evaluation_scorecards", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"interview_id", "interviewer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationScorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(name = "overall_score")
    private Double overallScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeedbackRecommendation recommendation;

    @Column(name = "overall_comments", columnDefinition = "TEXT")
    private String overallComments;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Builder.Default
    @OneToMany(mappedBy = "scorecard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScorecardEntry> entries = new ArrayList<>();

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Calculates the weighted overall score from all entries.
     */
    public Double calculateWeightedScore() {
        if (entries == null || entries.isEmpty()) return 0.0;

        double totalWeightedScore = 0;
        double totalWeight = 0;

        for (ScorecardEntry entry : entries) {
            double weight = entry.getCriteria().getWeight();
            double normalizedScore = (double) entry.getScore() / entry.getCriteria().getMaxScore() * 5.0;
            totalWeightedScore += normalizedScore * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? Math.round(totalWeightedScore / totalWeight * 10.0) / 10.0 : 0.0;
    }
}


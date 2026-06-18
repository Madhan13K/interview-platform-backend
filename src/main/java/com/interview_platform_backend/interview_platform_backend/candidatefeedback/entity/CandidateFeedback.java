package com.interview_platform_backend.interview_platform_backend.candidatefeedback.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"interview_id", "candidate_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Column(nullable = false)
    private Integer overallRating;

    private Integer communicationRating;

    private Integer professionalismRating;

    private Integer technicalClarityRating;

    private Integer timelinessRating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    private Boolean wouldRecommend;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAnonymous = false;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}

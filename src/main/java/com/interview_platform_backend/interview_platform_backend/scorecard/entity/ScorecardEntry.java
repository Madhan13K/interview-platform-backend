package com.interview_platform_backend.interview_platform_backend.scorecard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A single score entry within a scorecard — one per evaluation criteria.
 */
@Entity
@Table(name = "scorecard_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScorecardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scorecard_id", nullable = false)
    private EvaluationScorecard scorecard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id", nullable = false)
    private EvaluationCriteria criteria;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 500)
    private String comments;
}


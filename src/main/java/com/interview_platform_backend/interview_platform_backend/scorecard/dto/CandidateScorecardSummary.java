package com.interview_platform_backend.interview_platform_backend.scorecard.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Aggregated scorecard summary for a candidate across all interviewers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateScorecardSummary {

    private UUID candidateId;
    private String candidateName;
    private UUID interviewId;
    private String interviewTitle;
    private Double averageOverallScore;
    private Integer totalScorecards;
    private Map<String, Double> averageScoreByCriteria;
    private Map<FeedbackRecommendation, Integer> recommendationBreakdown;
}


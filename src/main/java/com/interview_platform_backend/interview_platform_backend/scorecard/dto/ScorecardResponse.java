package com.interview_platform_backend.interview_platform_backend.scorecard.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScorecardResponse {

    private UUID id;
    private UUID interviewId;
    private String interviewTitle;
    private UUID interviewerId;
    private String interviewerName;
    private UUID candidateId;
    private String candidateName;
    private Double overallScore;
    private FeedbackRecommendation recommendation;
    private String overallComments;
    private String strengths;
    private String weaknesses;
    private List<ScoreEntryResponse> entries;
    private Instant submittedAt;
    private Instant updatedAt;
}


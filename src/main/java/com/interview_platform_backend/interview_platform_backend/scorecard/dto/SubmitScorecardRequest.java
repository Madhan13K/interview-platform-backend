package com.interview_platform_backend.interview_platform_backend.scorecard.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitScorecardRequest {

    @NotNull(message = "Interview ID is required")
    private UUID interviewId;

    @NotNull(message = "Recommendation is required")
    private FeedbackRecommendation recommendation;

    private String overallComments;

    private String strengths;

    private String weaknesses;

    @NotEmpty(message = "At least one score entry is required")
    private List<ScoreEntryRequest> entries;
}


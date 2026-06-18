package com.interview_platform_backend.interview_platform_backend.candidate.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitFeedbackRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotNull
    private FeedbackRecommendation recommendation;

    private String strengths;

    private String weaknesses;

    private String comments;
}


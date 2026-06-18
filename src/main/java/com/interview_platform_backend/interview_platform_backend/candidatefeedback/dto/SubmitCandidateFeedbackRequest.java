package com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitCandidateFeedbackRequest {

    @NotNull
    private UUID interviewId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer overallRating;

    @Min(1)
    @Max(5)
    private Integer communicationRating;

    @Min(1)
    @Max(5)
    private Integer professionalismRating;

    @Min(1)
    @Max(5)
    private Integer technicalClarityRating;

    @Min(1)
    @Max(5)
    private Integer timelinessRating;

    private String comments;

    private Boolean wouldRecommend;

    private Boolean isAnonymous;
}

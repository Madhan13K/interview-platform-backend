package com.interview_platform_backend.interview_platform_backend.scorecard.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCriteriaRequest {

    @NotBlank(message = "Criteria name is required")
    private String name;

    private String description;

    private InterviewType interviewType;

    @Min(1) @Max(10)
    private Integer maxScore;

    @Min(0)
    private Double weight;

    private Integer orderIndex;
}


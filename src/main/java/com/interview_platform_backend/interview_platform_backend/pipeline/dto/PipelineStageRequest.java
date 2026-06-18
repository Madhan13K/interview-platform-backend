package com.interview_platform_backend.interview_platform_backend.pipeline.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStageRequest {

    @NotBlank(message = "Stage name is required")
    private String name;

    private String description;

    @NotNull(message = "Order index is required")
    private Integer orderIndex;

    private InterviewType interviewType;

    private UUID templateId;

    private Integer durationMinutes;

    private Boolean isOptional;
}


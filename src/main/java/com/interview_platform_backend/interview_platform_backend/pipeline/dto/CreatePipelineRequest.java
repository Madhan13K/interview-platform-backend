package com.interview_platform_backend.interview_platform_backend.pipeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePipelineRequest {

    @NotBlank(message = "Pipeline name is required")
    private String name;

    private String description;

    private String department;

    @NotEmpty(message = "At least one stage is required")
    private List<PipelineStageRequest> stages;
}


package com.interview_platform_backend.interview_platform_backend.pipeline.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePipelineRequest {

    private String name;
    private String description;
    private String department;
    private Boolean isActive;
    private List<PipelineStageRequest> stages;
}


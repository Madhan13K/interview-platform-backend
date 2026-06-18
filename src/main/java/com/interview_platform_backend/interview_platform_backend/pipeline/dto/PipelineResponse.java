package com.interview_platform_backend.interview_platform_backend.pipeline.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineResponse {

    private UUID id;
    private String name;
    private String description;
    private String department;
    private Boolean isActive;
    private UUID createdById;
    private String createdByName;
    private List<PipelineStageResponse> stages;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PipelineStageResponse {
        private UUID id;
        private String name;
        private String description;
        private Integer orderIndex;
        private InterviewType interviewType;
        private UUID templateId;
        private String templateName;
        private Integer durationMinutes;
        private Boolean isOptional;
    }
}


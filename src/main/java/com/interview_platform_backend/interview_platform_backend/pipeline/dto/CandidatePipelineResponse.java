package com.interview_platform_backend.interview_platform_backend.pipeline.dto;

import com.interview_platform_backend.interview_platform_backend.pipeline.entity.CandidatePipelineStatus;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.StageStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatePipelineResponse {

    private UUID id;
    private UUID pipelineId;
    private String pipelineName;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private UUID currentStageId;
    private String currentStageName;
    private Integer currentStageOrder;
    private Integer totalStages;
    private CandidatePipelineStatus status;
    private String notes;
    private List<StageProgressResponse> stageProgress;
    private Instant startedAt;
    private Instant completedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StageProgressResponse {
        private UUID id;
        private UUID stageId;
        private String stageName;
        private Integer stageOrder;
        private StageStatus status;
        private UUID interviewId;
        private String feedback;
        private Instant startedAt;
        private Instant completedAt;
    }
}


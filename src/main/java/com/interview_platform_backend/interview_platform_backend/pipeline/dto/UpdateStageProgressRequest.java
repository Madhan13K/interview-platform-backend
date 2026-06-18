package com.interview_platform_backend.interview_platform_backend.pipeline.dto;

import com.interview_platform_backend.interview_platform_backend.pipeline.entity.StageStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStageProgressRequest {

    private StageStatus status;
    private UUID interviewId;
    private String feedback;
}


package com.interview_platform_backend.interview_platform_backend.pipeline.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddCandidateToPipelineRequest {

    @NotNull(message = "Pipeline ID is required")
    private UUID pipelineId;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    private String notes;
}


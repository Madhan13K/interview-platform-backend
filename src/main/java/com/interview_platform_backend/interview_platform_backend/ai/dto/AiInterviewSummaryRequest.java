package com.interview_platform_backend.interview_platform_backend.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInterviewSummaryRequest {

    @NotNull(message = "Interview ID is required")
    private UUID interviewId;
}

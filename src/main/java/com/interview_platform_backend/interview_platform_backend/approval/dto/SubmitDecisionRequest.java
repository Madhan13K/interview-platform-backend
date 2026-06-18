package com.interview_platform_backend.interview_platform_backend.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitDecisionRequest {

    @NotNull(message = "Decision (approved) is required")
    private Boolean approved;

    private String comments;
}

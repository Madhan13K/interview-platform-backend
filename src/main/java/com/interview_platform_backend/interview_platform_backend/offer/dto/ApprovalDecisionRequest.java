package com.interview_platform_backend.interview_platform_backend.offer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDecisionRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    private String comments;
}

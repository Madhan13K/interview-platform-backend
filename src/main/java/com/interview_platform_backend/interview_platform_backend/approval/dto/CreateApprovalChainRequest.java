package com.interview_platform_backend.interview_platform_backend.approval.dto;

import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalEntityType;
import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApprovalChainRequest {

    @NotBlank(message = "Chain name is required")
    private String name;

    @NotNull(message = "Entity type is required")
    private ApprovalEntityType entityType;

    @NotNull(message = "Approval mode is required")
    private ApprovalMode approvalMode;

    @NotEmpty(message = "At least one step is required")
    @Valid
    private List<StepRequest> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepRequest {
        private int stepOrder;

        @NotBlank(message = "Approver role is required")
        private String approverRole;

        private UUID approverId;

        @Builder.Default
        private boolean required = true;
    }
}

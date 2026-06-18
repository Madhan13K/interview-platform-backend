package com.interview_platform_backend.interview_platform_backend.approval.dto;

import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalEntityType;
import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalChainResponse {

    private UUID id;
    private String name;
    private ApprovalEntityType entityType;
    private ApprovalMode approvalMode;
    private boolean active;
    private UUID tenantId;
    private UUID createdBy;
    private Instant createdAt;
    private List<StepResponse> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepResponse {
        private UUID id;
        private int stepOrder;
        private String approverRole;
        private UUID approverId;
        private boolean required;
    }
}

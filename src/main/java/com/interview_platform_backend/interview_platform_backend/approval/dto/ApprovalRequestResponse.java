package com.interview_platform_backend.interview_platform_backend.approval.dto;

import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalEntityType;
import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalRequestStatus;
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
public class ApprovalRequestResponse {

    private UUID id;
    private UUID chainId;
    private String chainName;
    private ApprovalEntityType entityType;
    private UUID entityId;
    private ApprovalRequestStatus status;
    private String requestedByEmail;
    private Instant requestedAt;
    private Instant completedAt;
    private List<DecisionResponse> decisions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DecisionResponse {
        private UUID id;
        private UUID stepId;
        private int stepOrder;
        private String approverRole;
        private String approverEmail;
        private boolean decision;
        private String comments;
        private Instant decidedAt;
    }
}

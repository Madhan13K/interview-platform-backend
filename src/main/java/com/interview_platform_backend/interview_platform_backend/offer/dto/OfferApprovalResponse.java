package com.interview_platform_backend.interview_platform_backend.offer.dto;

import com.interview_platform_backend.interview_platform_backend.offer.entity.ApprovalStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferApprovalResponse {

    private UUID id;
    private UUID approverId;
    private String approverName;
    private String approverEmail;
    private ApprovalStatus status;
    private String comments;
    private int approvalOrder;
    private Instant requestedAt;
    private Instant respondedAt;
}

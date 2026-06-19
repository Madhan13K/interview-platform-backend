package com.interview_platform_backend.interview_platform_backend.approval.repository;

import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, UUID> {

    List<ApprovalDecision> findByRequestId(UUID requestId);

    List<ApprovalDecision> findByApproverId(UUID approverId);

    boolean existsByRequestIdAndStepId(UUID requestId, UUID stepId);

    long countByRequestIdAndDecisionTrue(UUID requestId);

    long countByRequestIdAndDecisionFalse(UUID requestId);
}

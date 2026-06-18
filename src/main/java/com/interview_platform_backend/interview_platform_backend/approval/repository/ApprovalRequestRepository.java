package com.interview_platform_backend.interview_platform_backend.approval.repository;

import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalEntityType;
import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalRequest;
import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    List<ApprovalRequest> findByEntityTypeAndEntityId(ApprovalEntityType entityType, UUID entityId);

    List<ApprovalRequest> findByStatus(ApprovalRequestStatus status);

    List<ApprovalRequest> findByRequestedById(UUID userId);

    boolean existsByEntityTypeAndEntityIdAndStatusIn(ApprovalEntityType entityType, UUID entityId, List<ApprovalRequestStatus> statuses);
}

package com.interview_platform_backend.interview_platform_backend.approval.repository;

import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalChain;
import com.interview_platform_backend.interview_platform_backend.approval.entity.ApprovalEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalChainRepository extends JpaRepository<ApprovalChain, UUID> {

    List<ApprovalChain> findByEntityTypeAndActiveTrue(ApprovalEntityType entityType);

    List<ApprovalChain> findByActiveTrue();

    List<ApprovalChain> findByTenantId(UUID tenantId);

    boolean existsByNameAndEntityType(String name, ApprovalEntityType entityType);
}

package com.interview_platform_backend.interview_platform_backend.workflow.repository;

import com.interview_platform_backend.interview_platform_backend.workflow.entity.TriggerEvent;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowRuleRepository extends JpaRepository<WorkflowRule, UUID> {

    List<WorkflowRule> findByTriggerEventAndEnabledTrueOrderByPriorityDesc(TriggerEvent triggerEvent);

    List<WorkflowRule> findByEnabledTrueOrderByPriorityDesc();

    List<WorkflowRule> findByTenantIdOrderByPriorityDesc(UUID tenantId);

    List<WorkflowRule> findByCreatedByIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT r FROM WorkflowRule r WHERE r.triggerEvent = :event AND r.enabled = true " +
            "AND (r.tenantId IS NULL OR r.tenantId = :tenantId) ORDER BY r.priority DESC")
    List<WorkflowRule> findActiveRulesForEventAndTenant(
            @Param("event") TriggerEvent event,
            @Param("tenantId") UUID tenantId);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean existsByName(String name);
}

package com.interview_platform_backend.interview_platform_backend.workflow.repository;

import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowExecution;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowExecution.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    Page<WorkflowExecution> findByWorkflowRuleIdOrderByExecutedAtDesc(UUID ruleId, Pageable pageable);

    List<WorkflowExecution> findByTriggerEntityIdOrderByExecutedAtDesc(UUID entityId);

    List<WorkflowExecution> findByStatusOrderByExecutedAtDesc(ExecutionStatus status);

    @Query("SELECT e FROM WorkflowExecution e WHERE e.executedAt >= :from AND e.executedAt <= :to " +
            "ORDER BY e.executedAt DESC")
    List<WorkflowExecution> findByDateRange(@Param("from") Instant from, @Param("to") Instant to);

    long countByWorkflowRuleIdAndStatus(UUID ruleId, ExecutionStatus status);
}

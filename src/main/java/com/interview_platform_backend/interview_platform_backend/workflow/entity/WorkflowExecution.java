package com.interview_platform_backend.interview_platform_backend.workflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_executions", indexes = {
        @Index(name = "idx_workflow_executions_rule_id", columnList = "workflow_rule_id"),
        @Index(name = "idx_workflow_executions_status", columnList = "status"),
        @Index(name = "idx_workflow_executions_executed_at", columnList = "executedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_rule_id", nullable = false)
    private WorkflowRule workflowRule;

    @Column(nullable = false)
    private String triggerEntityType;

    @Column(nullable = false)
    private UUID triggerEntityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    private String executionResult;

    @Column(nullable = false)
    private Instant executedAt;

    private Long durationMs;

    public enum ExecutionStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }
}

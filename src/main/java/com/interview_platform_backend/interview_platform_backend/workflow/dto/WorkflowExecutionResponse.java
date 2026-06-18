package com.interview_platform_backend.interview_platform_backend.workflow.dto;

import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowExecution.ExecutionStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecutionResponse {

    private UUID id;
    private UUID workflowRuleId;
    private String workflowRuleName;
    private String triggerEntityType;
    private UUID triggerEntityId;
    private ExecutionStatus status;
    private String executionResult;
    private Instant executedAt;
    private Long durationMs;
}

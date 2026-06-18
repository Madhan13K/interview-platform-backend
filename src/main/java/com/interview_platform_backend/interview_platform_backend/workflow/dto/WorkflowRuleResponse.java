package com.interview_platform_backend.interview_platform_backend.workflow.dto;

import com.interview_platform_backend.interview_platform_backend.workflow.entity.ActionType;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.ConditionType;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.TriggerEvent;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowRuleResponse {

    private UUID id;
    private String name;
    private String description;
    private boolean enabled;
    private TriggerEvent triggerEvent;
    private ConditionType conditionType;
    private String conditionValue;
    private ActionType actionType;
    private String actionConfig;
    private int priority;
    private UUID tenantId;
    private UUID createdById;
    private String createdByName;
    private long executionCount;
    private Instant lastExecutedAt;
    private Instant createdAt;
    private Instant updatedAt;
}

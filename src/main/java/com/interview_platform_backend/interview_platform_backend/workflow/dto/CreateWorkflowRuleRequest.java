package com.interview_platform_backend.interview_platform_backend.workflow.dto;

import com.interview_platform_backend.interview_platform_backend.workflow.entity.ActionType;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.ConditionType;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.TriggerEvent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotNull(message = "Trigger event is required")
    private TriggerEvent triggerEvent;

    @NotNull(message = "Condition type is required")
    private ConditionType conditionType;

    @NotBlank(message = "Condition value is required")
    private String conditionValue;

    @NotNull(message = "Action type is required")
    private ActionType actionType;

    private String actionConfig;

    @Min(value = 0, message = "Priority must be non-negative")
    private int priority = 0;

    private UUID tenantId;

    private boolean enabled = true;
}

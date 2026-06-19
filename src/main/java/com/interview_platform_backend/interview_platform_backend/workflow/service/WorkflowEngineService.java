package com.interview_platform_backend.interview_platform_backend.workflow.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.workflow.dto.CreateWorkflowRuleRequest;
import com.interview_platform_backend.interview_platform_backend.workflow.dto.WorkflowExecutionResponse;
import com.interview_platform_backend.interview_platform_backend.workflow.dto.WorkflowRuleResponse;
import com.interview_platform_backend.interview_platform_backend.workflow.engine.WorkflowActionExecutor;
import com.interview_platform_backend.interview_platform_backend.workflow.engine.WorkflowConditionEvaluator;
import com.interview_platform_backend.interview_platform_backend.workflow.engine.WorkflowContext;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.TriggerEvent;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowExecution;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowExecution.ExecutionStatus;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowRule;
import com.interview_platform_backend.interview_platform_backend.workflow.repository.WorkflowExecutionRepository;
import com.interview_platform_backend.interview_platform_backend.workflow.repository.WorkflowRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);

    private final WorkflowRuleRepository ruleRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowConditionEvaluator conditionEvaluator;
    private final WorkflowActionExecutor actionExecutor;
    private final UserRepository userRepository;

    public WorkflowEngineService(WorkflowRuleRepository ruleRepository,
                                 WorkflowExecutionRepository executionRepository,
                                 WorkflowConditionEvaluator conditionEvaluator,
                                 WorkflowActionExecutor actionExecutor,
                                 UserRepository userRepository) {
        this.ruleRepository = ruleRepository;
        this.executionRepository = executionRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.actionExecutor = actionExecutor;
        this.userRepository = userRepository;
    }

    // ==================== Trigger Processing ====================

    /**
     * Processes a trigger event by finding matching rules, evaluating conditions, and executing actions.
     * This is the main entry point called from other services when events occur.
     */
    @Async
    public void processTrigger(TriggerEvent event, WorkflowContext context) {
        log.info("Processing workflow trigger: event={}, entityType={}, entityId={}",
                event, context.getEntityType(), context.getEntityId());

        context.setTriggerEvent(event);

        List<WorkflowRule> matchingRules = ruleRepository
                .findByTriggerEventAndEnabledTrueOrderByPriorityDesc(event);

        if (matchingRules.isEmpty()) {
            log.debug("No active workflow rules found for event: {}", event);
            return;
        }

        log.info("Found {} active rules for event {}", matchingRules.size(), event);

        for (WorkflowRule rule : matchingRules) {
            processRule(rule, context);
        }
    }

    private void processRule(WorkflowRule rule, WorkflowContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // Evaluate condition
            boolean conditionMet = conditionEvaluator.evaluateCondition(rule, context);

            if (!conditionMet) {
                log.debug("Condition not met for rule '{}', skipping", rule.getName());
                recordExecution(rule, context, ExecutionStatus.SKIPPED,
                        "Condition not met", System.currentTimeMillis() - startTime);
                return;
            }

            // Execute action
            log.info("Executing action for rule '{}': {}", rule.getName(), rule.getActionType());
            String result = actionExecutor.executeAction(rule, context);

            // Update rule execution stats
            rule.setExecutionCount(rule.getExecutionCount() + 1);
            rule.setLastExecutedAt(Instant.now());
            ruleRepository.save(rule);

            // Record successful execution
            recordExecution(rule, context, ExecutionStatus.SUCCESS,
                    result, System.currentTimeMillis() - startTime);

            log.info("Rule '{}' executed successfully: {}", rule.getName(), result);

        } catch (Exception e) {
            log.error("Error executing rule '{}': {}", rule.getName(), e.getMessage(), e);
            recordExecution(rule, context, ExecutionStatus.FAILED,
                    "Error: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Dry-run test of a rule. Evaluates the condition but does NOT execute the action.
     */
    @Transactional(readOnly = true)
    public WorkflowExecutionResponse testRule(UUID ruleId, WorkflowContext context) {
        WorkflowRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));

        long startTime = System.currentTimeMillis();

        boolean conditionMet = conditionEvaluator.evaluateCondition(rule, context);

        long durationMs = System.currentTimeMillis() - startTime;
        ExecutionStatus status = conditionMet ? ExecutionStatus.SUCCESS : ExecutionStatus.SKIPPED;
        String result = conditionMet
                ? "Condition met - action would be: " + rule.getActionType()
                : "Condition not met - action would be skipped";

        return WorkflowExecutionResponse.builder()
                .workflowRuleId(rule.getId())
                .workflowRuleName(rule.getName())
                .triggerEntityType(context.getEntityType())
                .triggerEntityId(context.getEntityId())
                .status(status)
                .executionResult("[DRY RUN] " + result)
                .executedAt(Instant.now())
                .durationMs(durationMs)
                .build();
    }

    private void recordExecution(WorkflowRule rule, WorkflowContext context,
                                 ExecutionStatus status, String result, long durationMs) {
        try {
            WorkflowExecution execution = WorkflowExecution.builder()
                    .workflowRule(rule)
                    .triggerEntityType(context.getEntityType() != null ? context.getEntityType() : "UNKNOWN")
                    .triggerEntityId(context.getEntityId() != null ? context.getEntityId() : UUID.randomUUID())
                    .status(status)
                    .executionResult(result)
                    .executedAt(Instant.now())
                    .durationMs(durationMs)
                    .build();
            executionRepository.save(execution);
        } catch (Exception e) {
            log.error("Failed to record workflow execution for rule '{}': {}", rule.getName(), e.getMessage());
        }
    }

    // ==================== Rule CRUD ====================

    public WorkflowRuleResponse createRule(CreateWorkflowRuleRequest request, UUID createdByUserId) {
        if (ruleRepository.existsByName(request.getName())) {
            throw new BadRequestException("A workflow rule with name '" + request.getName() + "' already exists");
        }

        User createdBy = null;
        if (createdByUserId != null) {
            createdBy = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdByUserId));
        }

        WorkflowRule rule = WorkflowRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .enabled(request.isEnabled())
                .triggerEvent(request.getTriggerEvent())
                .conditionType(request.getConditionType())
                .conditionValue(request.getConditionValue())
                .actionType(request.getActionType())
                .actionConfig(request.getActionConfig())
                .priority(request.getPriority())
                .tenantId(request.getTenantId())
                .createdBy(createdBy)
                .build();

        WorkflowRule saved = ruleRepository.save(rule);
        log.info("Created workflow rule '{}' (id={})", saved.getName(), saved.getId());
        return toRuleResponse(saved);
    }

    public WorkflowRuleResponse updateRule(UUID ruleId, CreateWorkflowRuleRequest request) {
        WorkflowRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));

        if (ruleRepository.existsByNameAndIdNot(request.getName(), ruleId)) {
            throw new BadRequestException("A workflow rule with name '" + request.getName() + "' already exists");
        }

        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setEnabled(request.isEnabled());
        rule.setTriggerEvent(request.getTriggerEvent());
        rule.setConditionType(request.getConditionType());
        rule.setConditionValue(request.getConditionValue());
        rule.setActionType(request.getActionType());
        rule.setActionConfig(request.getActionConfig());
        rule.setPriority(request.getPriority());
        rule.setTenantId(request.getTenantId());

        WorkflowRule saved = ruleRepository.save(rule);
        log.info("Updated workflow rule '{}' (id={})", saved.getName(), saved.getId());
        return toRuleResponse(saved);
    }

    public void deleteRule(UUID ruleId) {
        WorkflowRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));
        ruleRepository.delete(rule);
        log.info("Deleted workflow rule '{}' (id={})", rule.getName(), ruleId);
    }

    @Transactional(readOnly = true)
    public WorkflowRuleResponse getRule(UUID ruleId) {
        WorkflowRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));
        return toRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<WorkflowRuleResponse> getRules() {
        return ruleRepository.findByEnabledTrueOrderByPriorityDesc().stream()
                .map(this::toRuleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkflowRuleResponse> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(this::toRuleResponse)
                .toList();
    }

    public WorkflowRuleResponse toggleRule(UUID ruleId) {
        WorkflowRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRule", "id", ruleId));

        rule.setEnabled(!rule.isEnabled());
        WorkflowRule saved = ruleRepository.save(rule);
        log.info("Toggled workflow rule '{}' (id={}) enabled={}", saved.getName(), saved.getId(), saved.isEnabled());
        return toRuleResponse(saved);
    }

    // ==================== Execution History ====================

    @Transactional(readOnly = true)
    public Page<WorkflowExecutionResponse> getExecutionHistory(UUID ruleId, Pageable pageable) {
        return executionRepository.findByWorkflowRuleIdOrderByExecutedAtDesc(ruleId, pageable)
                .map(this::toExecutionResponse);
    }

    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> getExecutionsByEntity(UUID entityId) {
        return executionRepository.findByTriggerEntityIdOrderByExecutedAtDesc(entityId).stream()
                .map(this::toExecutionResponse)
                .toList();
    }

    // ==================== Mapping ====================

    private WorkflowRuleResponse toRuleResponse(WorkflowRule rule) {
        return WorkflowRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .enabled(rule.isEnabled())
                .triggerEvent(rule.getTriggerEvent())
                .conditionType(rule.getConditionType())
                .conditionValue(rule.getConditionValue())
                .actionType(rule.getActionType())
                .actionConfig(rule.getActionConfig())
                .priority(rule.getPriority())
                .tenantId(rule.getTenantId())
                .createdById(rule.getCreatedBy() != null ? rule.getCreatedBy().getId() : null)
                .createdByName(rule.getCreatedBy() != null
                        ? rule.getCreatedBy().getFirstName() + " " + rule.getCreatedBy().getLastName()
                        : null)
                .executionCount(rule.getExecutionCount())
                .lastExecutedAt(rule.getLastExecutedAt())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private WorkflowExecutionResponse toExecutionResponse(WorkflowExecution execution) {
        return WorkflowExecutionResponse.builder()
                .id(execution.getId())
                .workflowRuleId(execution.getWorkflowRule().getId())
                .workflowRuleName(execution.getWorkflowRule().getName())
                .triggerEntityType(execution.getTriggerEntityType())
                .triggerEntityId(execution.getTriggerEntityId())
                .status(execution.getStatus())
                .executionResult(execution.getExecutionResult())
                .executedAt(execution.getExecutedAt())
                .durationMs(execution.getDurationMs())
                .build();
    }
}

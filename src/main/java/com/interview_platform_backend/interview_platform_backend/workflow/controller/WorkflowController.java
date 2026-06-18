package com.interview_platform_backend.interview_platform_backend.workflow.controller;

import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.workflow.dto.CreateWorkflowRuleRequest;
import com.interview_platform_backend.interview_platform_backend.workflow.dto.WorkflowExecutionResponse;
import com.interview_platform_backend.interview_platform_backend.workflow.dto.WorkflowRuleResponse;
import com.interview_platform_backend.interview_platform_backend.workflow.engine.WorkflowContext;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.TriggerEvent;
import com.interview_platform_backend.interview_platform_backend.workflow.service.WorkflowEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow Engine", description = "Configurable rule-based automation engine")
public class WorkflowController {

    private final WorkflowEngineService workflowEngineService;
    private final SecurityHelper securityHelper;

    public WorkflowController(WorkflowEngineService workflowEngineService, SecurityHelper securityHelper) {
        this.workflowEngineService = workflowEngineService;
        this.securityHelper = securityHelper;
    }

    // ==================== Rule CRUD ====================

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Create a workflow rule", description = "Creates a new automation rule for the workflow engine")
    public ResponseEntity<WorkflowRuleResponse> createRule(@Valid @RequestBody CreateWorkflowRuleRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowEngineService.createRule(request, userId));
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Update a workflow rule", description = "Updates an existing automation rule")
    public ResponseEntity<WorkflowRuleResponse> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody CreateWorkflowRuleRequest request) {
        return ResponseEntity.ok(workflowEngineService.updateRule(ruleId, request));
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Delete a workflow rule", description = "Deletes an automation rule")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        workflowEngineService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Get a workflow rule", description = "Returns a workflow rule by ID")
    public ResponseEntity<WorkflowRuleResponse> getRule(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(workflowEngineService.getRule(ruleId));
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Get all workflow rules", description = "Returns all automation rules")
    public ResponseEntity<List<WorkflowRuleResponse>> getAllRules(
            @RequestParam(required = false, defaultValue = "false") boolean includeDisabled) {
        if (includeDisabled) {
            return ResponseEntity.ok(workflowEngineService.getAllRules());
        }
        return ResponseEntity.ok(workflowEngineService.getRules());
    }

    @PatchMapping("/rules/{ruleId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Toggle a workflow rule", description = "Enables or disables a workflow rule")
    public ResponseEntity<WorkflowRuleResponse> toggleRule(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(workflowEngineService.toggleRule(ruleId));
    }

    // ==================== Execution History ====================

    @GetMapping("/rules/{ruleId}/executions")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Get execution history for a rule", description = "Returns paginated execution history for a specific rule")
    public ResponseEntity<Page<WorkflowExecutionResponse>> getExecutionHistory(
            @PathVariable UUID ruleId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workflowEngineService.getExecutionHistory(ruleId, pageable));
    }

    @GetMapping("/executions/entity/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Get executions by entity", description = "Returns all workflow executions triggered by a specific entity")
    public ResponseEntity<List<WorkflowExecutionResponse>> getExecutionsByEntity(@PathVariable UUID entityId) {
        return ResponseEntity.ok(workflowEngineService.getExecutionsByEntity(entityId));
    }

    // ==================== Testing ====================

    @PostMapping("/rules/{ruleId}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    @Operation(summary = "Dry-run test a rule", description = "Tests a rule by evaluating the condition without executing the action")
    public ResponseEntity<WorkflowExecutionResponse> testRule(
            @PathVariable UUID ruleId,
            @RequestBody TestRuleRequest request) {
        WorkflowContext context = WorkflowContext.builder()
                .triggerEvent(request.triggerEvent())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .interviewId(request.interviewId())
                .candidateId(request.candidateId())
                .candidatePipelineId(request.candidatePipelineId())
                .metadata(request.metadata() != null ? request.metadata() : Map.of())
                .build();

        return ResponseEntity.ok(workflowEngineService.testRule(ruleId, context));
    }

    // ==================== Request Records ====================

    public record TestRuleRequest(
            TriggerEvent triggerEvent,
            String entityType,
            UUID entityId,
            UUID interviewId,
            UUID candidateId,
            UUID candidatePipelineId,
            Map<String, Object> metadata
    ) {}
}

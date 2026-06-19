package com.interview_platform_backend.interview_platform_backend.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.pipeline.service.PipelineService;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.ActionType;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class WorkflowActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowActionExecutor.class);

    private final PipelineService pipelineService;
    private final EmailNotificationService emailNotificationService;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public WorkflowActionExecutor(PipelineService pipelineService,
                                  EmailNotificationService emailNotificationService,
                                  InterviewRepository interviewRepository,
                                  UserRepository userRepository,
                                  ObjectMapper objectMapper) {
        this.pipelineService = pipelineService;
        this.emailNotificationService = emailNotificationService;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the action defined in the workflow rule given the context.
     * Returns a string describing the result.
     */
    public String executeAction(WorkflowRule rule, WorkflowContext context) {
        ActionType actionType = rule.getActionType();
        String actionConfig = rule.getActionConfig();

        return switch (actionType) {
            case ADVANCE_PIPELINE_STAGE -> executeAdvancePipelineStage(context, actionConfig);
            case SEND_EMAIL -> executeSendEmail(context, actionConfig);
            case CHANGE_INTERVIEW_STATUS -> executeChangeInterviewStatus(context, actionConfig);
            case REJECT_CANDIDATE -> executeRejectCandidate(context, actionConfig);
            case NOTIFY_RECRUITER -> executeNotifyRecruiter(context, actionConfig);
            case WEBHOOK_CALL -> executeWebhookCall(context, actionConfig);
        };
    }

    private String executeAdvancePipelineStage(WorkflowContext context, String actionConfig) {
        UUID candidatePipelineId = context.getCandidatePipelineId();
        if (candidatePipelineId == null) {
            // Try from metadata
            Object cpId = context.getMetadata().get("candidatePipelineId");
            if (cpId != null) {
                candidatePipelineId = UUID.fromString(cpId.toString());
            }
        }

        if (candidatePipelineId == null) {
            throw new IllegalStateException("No candidatePipelineId available in context for ADVANCE_PIPELINE_STAGE");
        }

        String feedback = actionConfig != null ? parseFeedbackFromConfig(actionConfig) : "Auto-advanced by workflow rule";
        pipelineService.advanceToNextStage(candidatePipelineId, feedback);

        log.info("Advanced candidate pipeline {} to next stage", candidatePipelineId);
        return "Advanced candidate pipeline " + candidatePipelineId + " to next stage";
    }

    private String executeSendEmail(WorkflowContext context, String actionConfig) {
        if (actionConfig == null || actionConfig.isBlank()) {
            throw new IllegalStateException("SEND_EMAIL action requires actionConfig with email details");
        }

        try {
            JsonNode config = objectMapper.readTree(actionConfig);
            String to = resolveEmailRecipient(config, context);
            String subject = config.has("subject") ? config.get("subject").asText() : "Workflow Notification";
            String body = config.has("body") ? config.get("body").asText() : "An automated workflow action was triggered.";

            // Replace placeholders
            body = replacePlaceholders(body, context);
            subject = replacePlaceholders(subject, context);

            emailNotificationService.sendEmail(to, subject, body);
            log.info("Sent email to {} with subject '{}'", to, subject);
            return "Email sent to " + to + " with subject: " + subject;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse SEND_EMAIL actionConfig: " + e.getMessage(), e);
        }
    }

    private String executeChangeInterviewStatus(WorkflowContext context, String actionConfig) {
        UUID interviewId = context.getInterviewId();
        if (interviewId == null) {
            throw new IllegalStateException("No interviewId available in context for CHANGE_INTERVIEW_STATUS");
        }

        if (actionConfig == null || actionConfig.isBlank()) {
            throw new IllegalStateException("CHANGE_INTERVIEW_STATUS requires actionConfig with target status");
        }

        InterviewStatus targetStatus;
        try {
            // actionConfig can be just the status name or a JSON like {"status": "COMPLETED"}
            if (actionConfig.trim().startsWith("{")) {
                JsonNode config = objectMapper.readTree(actionConfig);
                targetStatus = InterviewStatus.valueOf(config.get("status").asText().toUpperCase());
            } else {
                targetStatus = InterviewStatus.valueOf(actionConfig.trim().toUpperCase());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Invalid target status in actionConfig: " + actionConfig, e);
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new IllegalStateException("Interview not found: " + interviewId));

        interview.setStatus(targetStatus);
        interviewRepository.save(interview);

        log.info("Changed interview {} status to {}", interviewId, targetStatus);
        return "Interview " + interviewId + " status changed to " + targetStatus;
    }

    private String executeRejectCandidate(WorkflowContext context, String actionConfig) {
        UUID candidatePipelineId = context.getCandidatePipelineId();
        if (candidatePipelineId == null) {
            Object cpId = context.getMetadata().get("candidatePipelineId");
            if (cpId != null) {
                candidatePipelineId = UUID.fromString(cpId.toString());
            }
        }

        if (candidatePipelineId == null) {
            throw new IllegalStateException("No candidatePipelineId available in context for REJECT_CANDIDATE");
        }

        String feedback = actionConfig != null ? parseFeedbackFromConfig(actionConfig) : "Auto-rejected by workflow rule";
        pipelineService.rejectCandidate(candidatePipelineId, feedback);

        log.info("Rejected candidate in pipeline {}", candidatePipelineId);
        return "Candidate rejected in pipeline " + candidatePipelineId;
    }

    private String executeNotifyRecruiter(WorkflowContext context, String actionConfig) {
        UUID interviewId = context.getInterviewId();
        Interview interview = null;
        if (interviewId != null) {
            interview = interviewRepository.findByIdWithDetails(interviewId).orElse(null);
        }

        String recruiterEmail;
        String subject;
        String body;

        if (actionConfig != null && !actionConfig.isBlank()) {
            try {
                JsonNode config = objectMapper.readTree(actionConfig);
                recruiterEmail = config.has("recruiterEmail") ? config.get("recruiterEmail").asText() : null;
                subject = config.has("subject") ? config.get("subject").asText() : "Workflow Notification for Recruiter";
                body = config.has("body") ? config.get("body").asText() : "A workflow event requires your attention.";
            } catch (Exception e) {
                recruiterEmail = null;
                subject = "Workflow Notification for Recruiter";
                body = "A workflow event requires your attention.";
            }
        } else {
            subject = "Workflow Notification for Recruiter";
            body = "A workflow event requires your attention.";
            recruiterEmail = null;
        }

        // If no recruiter email in config, try to get from interview's scheduledBy
        if (recruiterEmail == null && interview != null && interview.getScheduledBy() != null) {
            recruiterEmail = interview.getScheduledBy().getEmail();
        }

        if (recruiterEmail == null) {
            throw new IllegalStateException("Unable to determine recruiter email for NOTIFY_RECRUITER action");
        }

        body = replacePlaceholders(body, context);
        subject = replacePlaceholders(subject, context);
        emailNotificationService.sendEmail(recruiterEmail, subject, body);

        log.info("Notified recruiter {} with subject '{}'", recruiterEmail, subject);
        return "Recruiter notified at " + recruiterEmail;
    }

    private String executeWebhookCall(WorkflowContext context, String actionConfig) {
        if (actionConfig == null || actionConfig.isBlank()) {
            throw new IllegalStateException("WEBHOOK_CALL requires actionConfig with webhook URL");
        }

        try {
            JsonNode config = objectMapper.readTree(actionConfig);
            String url = config.has("url") ? config.get("url").asText() : null;
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("Webhook URL is required in actionConfig");
            }

            String method = config.has("method") ? config.get("method").asText().toUpperCase() : "POST";

            // Build payload
            String payload = objectMapper.writeValueAsString(new WebhookPayload(
                    context.getTriggerEvent() != null ? context.getTriggerEvent().name() : null,
                    context.getEntityType(),
                    context.getEntityId(),
                    context.getInterviewId(),
                    context.getCandidateId(),
                    context.getMetadata()
            ));

            RestTemplate restTemplate = new RestTemplate();
            if ("POST".equals(method)) {
                restTemplate.postForEntity(url, payload, String.class);
            } else {
                restTemplate.getForEntity(url, String.class);
            }

            log.info("Webhook called: {} {}", method, url);
            return "Webhook called: " + method + " " + url;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Webhook call failed: " + e.getMessage(), e);
        }
    }

    private String resolveEmailRecipient(JsonNode config, WorkflowContext context) {
        if (config.has("to")) {
            return config.get("to").asText();
        }

        // Try to resolve candidate email
        UUID candidateId = context.getCandidateId();
        if (candidateId != null) {
            User candidate = userRepository.findById(candidateId).orElse(null);
            if (candidate != null) {
                return candidate.getEmail();
            }
        }

        // Try from interview
        UUID interviewId = context.getInterviewId();
        if (interviewId != null) {
            Interview interview = interviewRepository.findByIdWithDetails(interviewId).orElse(null);
            if (interview != null && interview.getCandidate() != null) {
                return interview.getCandidate().getEmail();
            }
        }

        throw new IllegalStateException("Unable to resolve email recipient");
    }

    private String replacePlaceholders(String text, WorkflowContext context) {
        if (text == null) return null;

        text = text.replace("{{interviewId}}", context.getInterviewId() != null ? context.getInterviewId().toString() : "N/A");
        text = text.replace("{{candidateId}}", context.getCandidateId() != null ? context.getCandidateId().toString() : "N/A");
        text = text.replace("{{entityId}}", context.getEntityId() != null ? context.getEntityId().toString() : "N/A");
        text = text.replace("{{entityType}}", context.getEntityType() != null ? context.getEntityType() : "N/A");
        text = text.replace("{{triggerEvent}}", context.getTriggerEvent() != null ? context.getTriggerEvent().name() : "N/A");

        // Replace metadata placeholders: {{meta.key}}
        if (context.getMetadata() != null) {
            for (var entry : context.getMetadata().entrySet()) {
                text = text.replace("{{meta." + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }

        return text;
    }

    private String parseFeedbackFromConfig(String actionConfig) {
        if (actionConfig == null || actionConfig.isBlank()) {
            return null;
        }
        try {
            if (actionConfig.trim().startsWith("{")) {
                JsonNode config = objectMapper.readTree(actionConfig);
                return config.has("feedback") ? config.get("feedback").asText() : actionConfig;
            }
            return actionConfig;
        } catch (Exception e) {
            return actionConfig;
        }
    }

    private record WebhookPayload(
            String triggerEvent,
            String entityType,
            UUID entityId,
            UUID interviewId,
            UUID candidateId,
            java.util.Map<String, Object> metadata
    ) {}
}

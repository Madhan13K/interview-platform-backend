package com.interview_platform_backend.interview_platform_backend.workflow.engine;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewFeedBack;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewInterviewer;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.ConditionType;
import com.interview_platform_backend.interview_platform_backend.workflow.entity.WorkflowRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowConditionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowConditionEvaluator.class);

    private final InterviewRepository interviewRepository;

    public WorkflowConditionEvaluator(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    /**
     * Evaluates whether the condition defined in the workflow rule is met given the context.
     */
    public boolean evaluateCondition(WorkflowRule rule, WorkflowContext context) {
        try {
            ConditionType conditionType = rule.getConditionType();
            String conditionValue = rule.getConditionValue();

            return switch (conditionType) {
                case SCORE_ABOVE -> evaluateScoreAbove(conditionValue, context);
                case SCORE_BELOW -> evaluateScoreBelow(conditionValue, context);
                case ALL_FEEDBACK_IN -> evaluateAllFeedbackIn(context);
                case RECOMMENDATION_COUNT -> evaluateRecommendationCount(conditionValue, context);
                case STATUS_EQUALS -> evaluateStatusEquals(conditionValue, context);
                case CUSTOM_EXPRESSION -> evaluateCustomExpression(conditionValue, context);
            };
        } catch (Exception e) {
            log.error("Error evaluating condition for rule '{}': {}", rule.getName(), e.getMessage(), e);
            return false;
        }
    }

    private boolean evaluateScoreAbove(String conditionValue, WorkflowContext context) {
        double threshold = Double.parseDouble(conditionValue);
        double avgScore = calculateAverageScore(context.getInterviewId());
        log.debug("SCORE_ABOVE evaluation: avgScore={}, threshold={}", avgScore, threshold);
        return avgScore > threshold;
    }

    private boolean evaluateScoreBelow(String conditionValue, WorkflowContext context) {
        double threshold = Double.parseDouble(conditionValue);
        double avgScore = calculateAverageScore(context.getInterviewId());
        log.debug("SCORE_BELOW evaluation: avgScore={}, threshold={}", avgScore, threshold);
        return avgScore < threshold;
    }

    private boolean evaluateAllFeedbackIn(WorkflowContext context) {
        UUID interviewId = context.getInterviewId();
        if (interviewId == null) {
            log.warn("ALL_FEEDBACK_IN: No interviewId in context");
            return false;
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId).orElse(null);
        if (interview == null) {
            log.warn("ALL_FEEDBACK_IN: Interview not found for id={}", interviewId);
            return false;
        }

        List<InterviewInterviewer> interviewers = interview.getInterviewers();
        List<InterviewFeedBack> feedbackList = interview.getFeedbackList();

        if (interviewers == null || interviewers.isEmpty()) {
            log.debug("ALL_FEEDBACK_IN: No interviewers assigned");
            return false;
        }

        int totalInterviewers = interviewers.size();
        int feedbackCount = feedbackList != null ? feedbackList.size() : 0;

        log.debug("ALL_FEEDBACK_IN: {}/{} feedback received", feedbackCount, totalInterviewers);
        return feedbackCount >= totalInterviewers;
    }

    /**
     * Evaluates recommendation count condition.
     * conditionValue format: "RECOMMENDATION:COUNT" e.g., "HIRE:2" means at least 2 HIRE recommendations.
     */
    private boolean evaluateRecommendationCount(String conditionValue, WorkflowContext context) {
        UUID interviewId = context.getInterviewId();
        if (interviewId == null) {
            log.warn("RECOMMENDATION_COUNT: No interviewId in context");
            return false;
        }

        String[] parts = conditionValue.split(":");
        if (parts.length != 2) {
            log.error("RECOMMENDATION_COUNT: Invalid conditionValue format '{}'. Expected 'RECOMMENDATION:COUNT'",
                    conditionValue);
            return false;
        }

        FeedbackRecommendation targetRecommendation;
        try {
            targetRecommendation = FeedbackRecommendation.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("RECOMMENDATION_COUNT: Invalid recommendation type '{}'", parts[0]);
            return false;
        }

        int requiredCount;
        try {
            requiredCount = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            log.error("RECOMMENDATION_COUNT: Invalid count '{}'", parts[1]);
            return false;
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId).orElse(null);
        if (interview == null || interview.getFeedbackList() == null) {
            return false;
        }

        long actualCount = interview.getFeedbackList().stream()
                .filter(fb -> fb.getRecommendation() == targetRecommendation)
                .count();

        log.debug("RECOMMENDATION_COUNT: target={}, required={}, actual={}",
                targetRecommendation, requiredCount, actualCount);
        return actualCount >= requiredCount;
    }

    /**
     * Evaluates whether the interview status equals the given value.
     */
    private boolean evaluateStatusEquals(String conditionValue, WorkflowContext context) {
        UUID interviewId = context.getInterviewId();
        if (interviewId == null) {
            // Try from metadata
            Object statusFromMetadata = context.getMetadata().get("currentStatus");
            if (statusFromMetadata != null) {
                return conditionValue.equalsIgnoreCase(statusFromMetadata.toString());
            }
            return false;
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId).orElse(null);
        if (interview == null) {
            return false;
        }

        return conditionValue.equalsIgnoreCase(interview.getStatus().name());
    }

    /**
     * Evaluates a custom expression. Currently supports simple key=value matching from metadata.
     */
    private boolean evaluateCustomExpression(String conditionValue, WorkflowContext context) {
        // Format: "key=value" or "key>value" or "key<value"
        if (conditionValue.contains("=")) {
            String[] parts = conditionValue.split("=", 2);
            Object metaValue = context.getMetadata().get(parts[0].trim());
            if (metaValue == null) {
                return false;
            }
            return parts[1].trim().equalsIgnoreCase(metaValue.toString());
        }
        log.warn("CUSTOM_EXPRESSION: Unsupported expression format '{}'", conditionValue);
        return false;
    }

    private double calculateAverageScore(UUID interviewId) {
        if (interviewId == null) {
            return 0.0;
        }

        Interview interview = interviewRepository.findByIdWithDetails(interviewId).orElse(null);
        if (interview == null || interview.getFeedbackList() == null || interview.getFeedbackList().isEmpty()) {
            return 0.0;
        }

        return interview.getFeedbackList().stream()
                .mapToInt(InterviewFeedBack::getRating)
                .average()
                .orElse(0.0);
    }
}

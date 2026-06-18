package com.interview_platform_backend.interview_platform_backend.notification.slack.service;

import com.interview_platform_backend.interview_platform_backend.notification.slack.dto.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SlackNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationService.class);

    @Value("${app.slack.webhook-url:}")
    private String webhookUrl;

    @Value("${app.slack.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendNotification(String channel, String message) {
        if (!enabled || webhookUrl.isBlank()) {
            log.debug("Slack notification skipped (disabled or no webhook URL)");
            return;
        }

        try {
            SlackMessage slackMessage = SlackMessage.builder()
                    .channel(channel)
                    .text(message)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SlackMessage> entity = new HttpEntity<>(slackMessage, headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("Slack notification sent to channel: {}", channel);
        } catch (Exception e) {
            log.error("Failed to send Slack notification: {}", e.getMessage());
        }
    }

    public void notifyInterviewScheduled(String channel, String candidateName, String interviewTitle, String date) {
        String message = String.format(":calendar: *Interview Scheduled*\n>Candidate: %s\n>Interview: %s\n>Date: %s",
                candidateName, interviewTitle, date);
        sendNotification(channel, message);
    }

    public void notifyFeedbackSubmitted(String channel, String interviewerName, String candidateName, int rating) {
        String message = String.format(":memo: *Feedback Submitted*\n>By: %s\n>Candidate: %s\n>Rating: %d/5",
                interviewerName, candidateName, rating);
        sendNotification(channel, message);
    }

    public void notifyCandidateHired(String channel, String candidateName, String position) {
        String message = String.format(":tada: *Candidate Hired!*\n>%s has been hired for %s",
                candidateName, position);
        sendNotification(channel, message);
    }

    public void notifyCandidateRejected(String channel, String candidateName, String reason) {
        String message = String.format(":x: *Candidate Rejected*\n>%s\n>Reason: %s",
                candidateName, reason);
        sendNotification(channel, message);
    }
}

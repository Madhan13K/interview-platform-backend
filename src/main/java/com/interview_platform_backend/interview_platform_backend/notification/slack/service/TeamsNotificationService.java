package com.interview_platform_backend.interview_platform_backend.notification.slack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TeamsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TeamsNotificationService.class);

    @Value("${app.teams.webhook-url:}")
    private String webhookUrl;

    @Value("${app.teams.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendNotification(String title, String message) {
        if (!enabled || webhookUrl.isBlank()) {
            log.debug("Teams notification skipped (disabled or no webhook URL)");
            return;
        }

        try {
            Map<String, Object> card = Map.of(
                    "@type", "MessageCard",
                    "@context", "http://schema.org/extensions",
                    "summary", title,
                    "themeColor", "0076D7",
                    "title", title,
                    "text", message
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(card, headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("Teams notification sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send Teams notification: {}", e.getMessage());
        }
    }
}

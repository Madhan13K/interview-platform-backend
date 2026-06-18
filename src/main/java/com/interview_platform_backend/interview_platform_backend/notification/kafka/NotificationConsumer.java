package com.interview_platform_backend.interview_platform_backend.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.notification.sms.SmsNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka consumer that processes notification messages and dispatches
 * to the appropriate channel (email, SMS, push notification).
 */
@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(EmailNotificationService emailService,
                                 SmsNotificationService smsService,
                                 ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS, groupId = "notification-service")
    public void consumeNotification(String message) {
        try {
            NotificationMessage notification = objectMapper.readValue(message, NotificationMessage.class);
            log.info("Received notification: type={}, recipient={}, channels={}",
                    notification.getEventType(),
                    notification.getRecipientEmail(),
                    notification.getChannels());

            for (String channel : notification.getChannels()) {
                switch (channel.toUpperCase()) {
                    case "EMAIL" -> dispatchEmail(notification);
                    case "SMS" -> dispatchSms(notification);
                    default -> log.warn("Unknown notification channel: {}", channel);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process notification message: {}", e.getMessage(), e);
        }
    }

    /**
     * Consumes interview lifecycle events (e.g., FEEDBACK_SUBMITTED).
     * These can be used for analytics, dashboards, or additional notifications.
     */
    @KafkaListener(topics = KafkaTopics.INTERVIEW_EVENTS, groupId = "notification-service")
    public void consumeInterviewEvent(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            log.info("Received interview event: {}", event);

            // Process interview events (analytics, real-time dashboard updates, etc.)
            // Currently logs; extend as needed for specific event types
        } catch (Exception e) {
            log.error("Failed to process interview event: {}", e.getMessage(), e);
        }
    }

    private void dispatchEmail(NotificationMessage notification) {
        if (notification.getRecipientEmail() != null && !notification.getRecipientEmail().isBlank()) {
            emailService.sendEmail(
                    notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getBody()
            );
        }
    }

    private void dispatchSms(NotificationMessage notification) {
        if (notification.getRecipientPhone() != null && !notification.getRecipientPhone().isBlank()) {
            String smsBody = truncateForSms(notification.getBody());
            smsService.sendSms(notification.getRecipientPhone(), smsBody);
        } else {
            log.warn("SMS requested but no phone number for recipient: {}", notification.getRecipientEmail());
        }
    }

    private String truncateForSms(String body) {
        if (body == null) return "";
        return body.length() > 160 ? body.substring(0, 157) + "..." : body;
    }
}


package com.interview_platform_backend.interview_platform_backend.notification.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes notification messages to Kafka topics.
 * Downstream consumers handle actual delivery (email, SMS, push).
 * If Kafka is disabled, logs the notification instead.
 */
@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public NotificationProducer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendNotification(NotificationMessage message) {
        if (!kafkaEnabled) {
            log.info("Kafka disabled. Notification: type={}, recipient={}, channels={}",
                    message.getEventType(), message.getRecipientEmail(), message.getChannels());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, message.getRecipientEmail(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send notification to Kafka: {}", ex.getMessage());
                        } else {
                            log.info("Notification sent to Kafka topic={}, key={}, offset={}",
                                    KafkaTopics.NOTIFICATION_EVENTS,
                                    message.getRecipientEmail(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification message: {}", e.getMessage());
        }
    }

    public void sendInterviewEvent(String eventType, String key, Object eventData) {
        if (!kafkaEnabled) {
            log.info("Kafka disabled. Interview event: type={}, key={}", eventType, key);
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send(KafkaTopics.INTERVIEW_EVENTS, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send interview event to Kafka: {}", ex.getMessage());
                        } else {
                            log.info("Interview event sent: type={}, key={}", eventType, key);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize interview event: {}", e.getMessage());
        }
    }
}


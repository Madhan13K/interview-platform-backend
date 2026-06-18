package com.interview_platform_backend.interview_platform_backend.notification.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaTopicConfig {

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic interviewEventsTopic() {
        return TopicBuilder.name(KafkaTopics.INTERVIEW_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}


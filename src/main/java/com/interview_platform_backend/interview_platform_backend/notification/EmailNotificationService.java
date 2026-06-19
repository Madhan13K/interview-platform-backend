package com.interview_platform_backend.interview_platform_backend.notification;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@interview-platform.com}")
    private String fromEmail;

    @Value("${app.notifications.enabled:false}")
    private boolean notificationsEnabled;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Retryable(
            retryFor = {MailException.class, Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 10000)
    )
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendEmailFallback")
    public void sendEmail(String to, String subject, String body) {
        if (!notificationsEnabled) {
            log.info("Email notifications disabled. Would send to={}, subject={}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (MailException e) {
            log.error("Failed to send email to {} (will retry): {}", to, e.getMessage());
            throw e;
        }
    }

    @Async
    public void sendEmailToMultiple(List<String> recipients, String subject, String body) {
        for (String recipient : recipients) {
            sendEmail(recipient, subject, body);
        }
    }

    @Recover
    private void sendEmailFallback(MailException ex, String to, String subject, String body) {
        log.error("All retry attempts exhausted for email to={}. subject={}. Error: {}",
                to, subject, ex.getMessage());
        // TODO: Push to dead letter queue (Kafka topic) for manual retry
    }

    private void sendEmailFallback(String to, String subject, String body, Throwable throwable) {
        log.warn("Circuit breaker open for email service. Failed to send email to={}, subject={}. Cause: {}",
                to, subject, throwable.getMessage());
    }
}


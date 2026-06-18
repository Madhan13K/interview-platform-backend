package com.interview_platform_backend.interview_platform_backend.notification;

import com.interview_platform_backend.interview_platform_backend.event.FeedbackSubmittedEvent;
import com.interview_platform_backend.interview_platform_backend.event.InterviewCancelledEvent;
import com.interview_platform_backend.interview_platform_backend.event.InterviewRescheduledEvent;
import com.interview_platform_backend.interview_platform_backend.event.InterviewScheduledEvent;
import com.interview_platform_backend.interview_platform_backend.notification.kafka.NotificationMessage;
import com.interview_platform_backend.interview_platform_backend.notification.kafka.NotificationProducer;
import com.interview_platform_backend.interview_platform_backend.notification.service.InAppNotificationService;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to application events and publishes notification messages to Kafka.
 * Also creates in-app notifications and pushes via WebSocket.
 */
@Component
public class InterviewEventListener {

    private static final Logger log = LoggerFactory.getLogger(InterviewEventListener.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
            .withZone(ZoneId.systemDefault());

    private final NotificationProducer notificationProducer;
    private final InAppNotificationService inAppNotificationService;
    private final UserRepository userRepository;

    public InterviewEventListener(NotificationProducer notificationProducer,
                                  InAppNotificationService inAppNotificationService,
                                  UserRepository userRepository) {
        this.notificationProducer = notificationProducer;
        this.inAppNotificationService = inAppNotificationService;
        this.userRepository = userRepository;
    }

    @Async
    @EventListener
    public void handleInterviewScheduled(InterviewScheduledEvent event) {
        log.info("Interview scheduled event: interviewId={}, candidate={}", event.getInterviewId(), event.getCandidateEmail());

        // Notify candidate via Kafka (email + SMS)
        String candidateBody = String.format("""
                Hi %s,
                
                Your interview "%s" has been scheduled.
                
                Date: %s - %s
                Scheduled by: %s
                
                Please be prepared and join on time.
                
                Best regards,
                Interview Platform Team
                """,
                event.getCandidateName(),
                event.getTitle(),
                FORMATTER.format(event.getStartTime()),
                FORMATTER.format(event.getEndTime()),
                event.getScheduledByName());

        notificationProducer.sendNotification(NotificationMessage.builder()
                .eventType("INTERVIEW_SCHEDULED")
                .channels(List.of("EMAIL", "SMS"))
                .recipientEmail(event.getCandidateEmail())
                .recipientName(event.getCandidateName())
                .subject("Interview Scheduled: " + event.getTitle())
                .body(candidateBody)
                .metadata(Map.of("interviewId", event.getInterviewId().toString()))
                .timestamp(Instant.now())
                .build());

        // Notify each interviewer via Kafka (email only)
        String interviewerBody = String.format("""
                Hi,
                
                You have been assigned as an interviewer for "%s".
                
                Candidate: %s
                Date: %s - %s
                
                Please review the candidate's profile before the interview.
                
                Best regards,
                Interview Platform Team
                """,
                event.getTitle(),
                event.getCandidateName(),
                FORMATTER.format(event.getStartTime()),
                FORMATTER.format(event.getEndTime()));

        for (String interviewerEmail : event.getInterviewerEmails()) {
            notificationProducer.sendNotification(NotificationMessage.builder()
                    .eventType("INTERVIEW_ASSIGNED")
                    .channels(List.of("EMAIL"))
                    .recipientEmail(interviewerEmail)
                    .subject("Interview Assignment: " + event.getTitle())
                    .body(interviewerBody)
                    .metadata(Map.of("interviewId", event.getInterviewId().toString()))
                    .timestamp(Instant.now())
                    .build());
        }

        // In-app notifications
        pushInAppNotification(event.getCandidateEmail(), "INTERVIEW_SCHEDULED",
                "Interview Scheduled", "Your interview \"" + event.getTitle() + "\" is scheduled for " + FORMATTER.format(event.getStartTime()),
                event.getInterviewId(), "INTERVIEW");

        for (String interviewerEmail : event.getInterviewerEmails()) {
            pushInAppNotification(interviewerEmail, "INTERVIEW_ASSIGNED",
                    "New Interview Assignment", "You've been assigned to interview: " + event.getTitle(),
                    event.getInterviewId(), "INTERVIEW");
        }
    }

    @Async
    @EventListener
    public void handleInterviewRescheduled(InterviewRescheduledEvent event) {
        log.info("Interview rescheduled event: interviewId={}", event.getInterviewId());

        String body = String.format("""
                Hi %s,
                
                Your interview "%s" has been rescheduled.
                
                New Date: %s - %s
                Reason: %s
                
                Please update your calendar accordingly.
                
                Best regards,
                Interview Platform Team
                """,
                event.getCandidateName(),
                event.getTitle(),
                FORMATTER.format(event.getNewStartTime()),
                FORMATTER.format(event.getNewEndTime()),
                event.getReason() != null ? event.getReason() : "Schedule adjustment");

        // Notify candidate (email + SMS)
        notificationProducer.sendNotification(NotificationMessage.builder()
                .eventType("INTERVIEW_RESCHEDULED")
                .channels(List.of("EMAIL", "SMS"))
                .recipientEmail(event.getCandidateEmail())
                .recipientName(event.getCandidateName())
                .subject("Interview Rescheduled: " + event.getTitle())
                .body(body)
                .metadata(Map.of("interviewId", event.getInterviewId().toString()))
                .timestamp(Instant.now())
                .build());

        // Notify interviewers (email)
        for (String email : event.getInterviewerEmails()) {
            notificationProducer.sendNotification(NotificationMessage.builder()
                    .eventType("INTERVIEW_RESCHEDULED")
                    .channels(List.of("EMAIL"))
                    .recipientEmail(email)
                    .subject("Interview Rescheduled: " + event.getTitle())
                    .body(body)
                    .metadata(Map.of("interviewId", event.getInterviewId().toString()))
                    .timestamp(Instant.now())
                    .build());
        }

        // In-app notifications
        pushInAppNotification(event.getCandidateEmail(), "INTERVIEW_RESCHEDULED",
                "Interview Rescheduled", "Your interview \"" + event.getTitle() + "\" has been rescheduled to " + FORMATTER.format(event.getNewStartTime()),
                event.getInterviewId(), "INTERVIEW");
        for (String email : event.getInterviewerEmails()) {
            pushInAppNotification(email, "INTERVIEW_RESCHEDULED",
                    "Interview Rescheduled", "Interview \"" + event.getTitle() + "\" has been rescheduled",
                    event.getInterviewId(), "INTERVIEW");
        }
    }

    @Async
    @EventListener
    public void handleInterviewCancelled(InterviewCancelledEvent event) {
        log.info("Interview cancelled event: interviewId={}", event.getInterviewId());

        String body = String.format("""
                Hi %s,
                
                The interview "%s" has been cancelled.
                
                Reason: %s
                
                We apologize for any inconvenience.
                
                Best regards,
                Interview Platform Team
                """,
                event.getCandidateName(),
                event.getTitle(),
                event.getReason());

        // Notify candidate (email + SMS)
        notificationProducer.sendNotification(NotificationMessage.builder()
                .eventType("INTERVIEW_CANCELLED")
                .channels(List.of("EMAIL", "SMS"))
                .recipientEmail(event.getCandidateEmail())
                .recipientName(event.getCandidateName())
                .subject("Interview Cancelled: " + event.getTitle())
                .body(body)
                .metadata(Map.of("interviewId", event.getInterviewId().toString()))
                .timestamp(Instant.now())
                .build());

        // Notify interviewers (email)
        for (String email : event.getInterviewerEmails()) {
            notificationProducer.sendNotification(NotificationMessage.builder()
                    .eventType("INTERVIEW_CANCELLED")
                    .channels(List.of("EMAIL"))
                    .recipientEmail(email)
                    .subject("Interview Cancelled: " + event.getTitle())
                    .body(body)
                    .metadata(Map.of("interviewId", event.getInterviewId().toString()))
                    .timestamp(Instant.now())
                    .build());
        }

        // In-app notifications
        pushInAppNotification(event.getCandidateEmail(), "INTERVIEW_CANCELLED",
                "Interview Cancelled", "Your interview \"" + event.getTitle() + "\" has been cancelled. Reason: " + event.getReason(),
                event.getInterviewId(), "INTERVIEW");
        for (String email : event.getInterviewerEmails()) {
            pushInAppNotification(email, "INTERVIEW_CANCELLED",
                    "Interview Cancelled", "Interview \"" + event.getTitle() + "\" has been cancelled",
                    event.getInterviewId(), "INTERVIEW");
        }
    }

    @Async
    @EventListener
    public void handleFeedbackSubmitted(FeedbackSubmittedEvent event) {
        log.info("Feedback submitted: interviewId={}, by={}, rating={}, recommendation={}",
                event.getInterviewId(), event.getInterviewerName(),
                event.getRating(), event.getRecommendation());

        // Notify recruiters/admins about new feedback (could be extended)
        notificationProducer.sendInterviewEvent("FEEDBACK_SUBMITTED",
                event.getInterviewId().toString(),
                Map.of(
                        "interviewId", event.getInterviewId().toString(),
                        "interviewTitle", event.getInterviewTitle(),
                        "interviewer", event.getInterviewerName(),
                        "candidate", event.getCandidateName(),
                        "rating", event.getRating().toString(),
                        "recommendation", event.getRecommendation().name()
                ));
    }

    // ---- Helper ----

    private void pushInAppNotification(String email, String type, String title, String message,
                                       UUID referenceId, String referenceType) {
        try {
            userRepository.findByEmail(email).ifPresent(user ->
                    inAppNotificationService.notify(user.getId(), type, title, message, referenceId, referenceType)
            );
        } catch (Exception e) {
            log.warn("Failed to push in-app notification to {}: {}", email, e.getMessage());
        }
    }
}

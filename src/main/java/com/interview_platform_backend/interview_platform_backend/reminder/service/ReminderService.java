package com.interview_platform_backend.interview_platform_backend.reminder.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewInterviewer;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.reminder.entity.InterviewReminder;
import com.interview_platform_backend.interview_platform_backend.reminder.entity.InterviewReminder.*;
import com.interview_platform_backend.interview_platform_backend.reminder.repository.InterviewReminderRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final InterviewReminderRepository reminderRepository;
    private final InterviewRepository interviewRepository;
    private final EmailNotificationService emailNotificationService;

    public ReminderService(InterviewReminderRepository reminderRepository,
                            InterviewRepository interviewRepository,
                            EmailNotificationService emailNotificationService) {
        this.reminderRepository = reminderRepository;
        this.interviewRepository = interviewRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Auto-create reminders when an interview is scheduled.
     */
    public void createRemindersForInterview(UUID interviewId) {
        Interview interview = interviewRepository.findByIdWithDetails(interviewId).orElse(null);
        if (interview == null) return;

        List<User> recipients = new ArrayList<>();
        recipients.add(interview.getCandidate());
        if (interview.getInterviewers() != null) {
            for (InterviewInterviewer ii : interview.getInterviewers()) {
                recipients.add(ii.getInterviewer());
            }
        }

        Instant interviewTime = interview.getStartTime();
        for (User recipient : recipients) {
            createReminder(interview, recipient, ReminderType.BEFORE_24H, interviewTime.minus(24, ChronoUnit.HOURS));
            createReminder(interview, recipient, ReminderType.BEFORE_1H, interviewTime.minus(1, ChronoUnit.HOURS));
            createReminder(interview, recipient, ReminderType.BEFORE_15MIN, interviewTime.minus(15, ChronoUnit.MINUTES));
        }

        log.info("Created reminders for interview {} with {} recipients", interviewId, recipients.size());
    }

    /**
     * Cancel all pending reminders for an interview (e.g., when cancelled).
     */
    public void cancelRemindersForInterview(UUID interviewId) {
        List<InterviewReminder> pending = reminderRepository.findPendingByInterview(interviewId);
        for (InterviewReminder r : pending) {
            r.setStatus(ReminderStatus.CANCELLED);
        }
        reminderRepository.saveAll(pending);
        log.info("Cancelled {} reminders for interview {}", pending.size(), interviewId);
    }

    /**
     * Scheduled job: process due reminders every minute.
     */
    @Scheduled(fixedRate = 60000) // every minute
    @SchedulerLock(name = "processDueReminders", lockAtLeastFor = "1m")
    public void processDueReminders() {
        List<InterviewReminder> dueReminders = reminderRepository.findPendingDueReminders(Instant.now());
        for (InterviewReminder reminder : dueReminders) {
            try {
                sendReminder(reminder);
                reminder.setStatus(ReminderStatus.SENT);
                reminder.setSentAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to send reminder {}: {}", reminder.getId(), e.getMessage());
                reminder.setStatus(ReminderStatus.FAILED);
            }
        }
        if (!dueReminders.isEmpty()) {
            reminderRepository.saveAll(dueReminders);
            log.info("Processed {} due reminders", dueReminders.size());
        }
    }

    @Transactional(readOnly = true)
    public List<InterviewReminder> getRemindersForInterview(UUID interviewId) {
        return reminderRepository.findByInterviewId(interviewId);
    }

    @Transactional(readOnly = true)
    public List<InterviewReminder> getRemindersForUser(UUID userId) {
        return reminderRepository.findByUserId(userId);
    }

    private void createReminder(Interview interview, User user, ReminderType type, Instant scheduledAt) {
        if (scheduledAt.isBefore(Instant.now())) return; // don't create past reminders

        InterviewReminder reminder = InterviewReminder.builder()
                .interview(interview)
                .user(user)
                .reminderType(type)
                .channel(ReminderChannel.EMAIL)
                .scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING)
                .build();
        reminderRepository.save(reminder);
    }

    private void sendReminder(InterviewReminder reminder) {
        Interview interview = reminder.getInterview();
        User user = reminder.getUser();
        String timeLabel = switch (reminder.getReminderType()) {
            case BEFORE_24H -> "24 hours";
            case BEFORE_1H -> "1 hour";
            case BEFORE_15MIN -> "15 minutes";
        };

        String subject = "⏰ Interview Reminder: " + interview.getTitle() + " in " + timeLabel;
        String body = String.format(
                "Hi %s,\n\nThis is a reminder that your interview \"%s\" starts in %s.\n\n" +
                "Time: %s\nMeeting Link: %s\n\nGood luck!\nInterview Platform Team",
                user.getFirstName(), interview.getTitle(), timeLabel,
                interview.getStartTime().toString(),
                interview.getMeetingLink() != null ? interview.getMeetingLink() : "N/A"
        );

        if (reminder.getChannel() == ReminderChannel.EMAIL) {
            emailNotificationService.sendEmail(user.getEmail(), subject, body);
        }
        // SMS and PUSH would be handled here with respective services
    }
}


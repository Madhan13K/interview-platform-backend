package com.interview_platform_backend.interview_platform_backend.reminder.controller;

import com.interview_platform_backend.interview_platform_backend.reminder.entity.InterviewReminder;
import com.interview_platform_backend.interview_platform_backend.reminder.service.ReminderService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@Tag(name = "Interview Reminders", description = "Scheduled reminders (24h, 1h, 15min before) via email/SMS/push")
public class ReminderController {

    private final ReminderService reminderService;
    private final SecurityHelper securityHelper;

    public ReminderController(ReminderService reminderService, SecurityHelper securityHelper) {
        this.reminderService = reminderService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Create reminders for an interview")
    @PostMapping("/interview/{interviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<String> createReminders(@PathVariable UUID interviewId) {
        reminderService.createRemindersForInterview(interviewId);
        return ResponseEntity.ok("Reminders created for interview " + interviewId);
    }

    @Operation(summary = "Cancel all reminders for an interview")
    @DeleteMapping("/interview/{interviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<Void> cancelReminders(@PathVariable UUID interviewId) {
        reminderService.cancelRemindersForInterview(interviewId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get reminders for an interview")
    @GetMapping("/interview/{interviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InterviewReminder>> getByInterview(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(reminderService.getRemindersForInterview(interviewId));
    }

    @Operation(summary = "Get my reminders")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InterviewReminder>> getMyReminders() {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(reminderService.getRemindersForUser(userId));
    }
}


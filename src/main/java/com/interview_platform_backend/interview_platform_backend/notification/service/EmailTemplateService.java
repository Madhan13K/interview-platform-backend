package com.interview_platform_backend.interview_platform_backend.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailTemplateService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailTemplateService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String htmlContent = templateEngine.process("email/" + templateName, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendInterviewInvite(String to, String candidateName, String jobTitle,
                                     String date, String time, String type, String mode, String meetingLink) {
        sendTemplatedEmail(to, "Interview Invitation - " + jobTitle, "interview-invite",
                Map.of("candidateName", candidateName, "jobTitle", jobTitle,
                        "interviewDate", date, "interviewTime", time,
                        "interviewType", type, "interviewMode", mode,
                        "meetingLink", meetingLink != null ? meetingLink : ""));
    }

    public void sendReminder(String to, String recipientName, String interviewTitle,
                             String timeUntil, String meetingLink) {
        sendTemplatedEmail(to, "Interview Reminder - " + interviewTitle, "reminder",
                Map.of("recipientName", recipientName, "interviewTitle", interviewTitle,
                        "timeUntil", timeUntil, "meetingLink", meetingLink != null ? meetingLink : ""));
    }

    public void sendVerificationEmail(String to, String userName, String verificationUrl) {
        sendTemplatedEmail(to, "Verify Your Email Address", "verification",
                Map.of("userName", userName, "verificationUrl", verificationUrl));
    }

    public void sendPasswordResetEmail(String to, String userName, String resetUrl) {
        sendTemplatedEmail(to, "Password Reset Request", "password-reset",
                Map.of("userName", userName, "resetUrl", resetUrl));
    }
}

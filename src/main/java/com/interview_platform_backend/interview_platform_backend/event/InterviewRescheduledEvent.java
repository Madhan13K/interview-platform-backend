package com.interview_platform_backend.interview_platform_backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class InterviewRescheduledEvent extends ApplicationEvent {

    private final UUID interviewId;
    private final String title;
    private final String candidateEmail;
    private final String candidateName;
    private final List<String> interviewerEmails;
    private final Instant newStartTime;
    private final Instant newEndTime;
    private final String reason;

    public InterviewRescheduledEvent(Object source, UUID interviewId, String title,
                                      String candidateEmail, String candidateName,
                                      List<String> interviewerEmails,
                                      Instant newStartTime, Instant newEndTime,
                                      String reason) {
        super(source);
        this.interviewId = interviewId;
        this.title = title;
        this.candidateEmail = candidateEmail;
        this.candidateName = candidateName;
        this.interviewerEmails = interviewerEmails;
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
        this.reason = reason;
    }
}


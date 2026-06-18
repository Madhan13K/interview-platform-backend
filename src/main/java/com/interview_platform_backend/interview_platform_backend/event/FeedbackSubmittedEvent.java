package com.interview_platform_backend.interview_platform_backend.event;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class FeedbackSubmittedEvent extends ApplicationEvent {

    private final UUID interviewId;
    private final String interviewTitle;
    private final String interviewerName;
    private final String interviewerEmail;
    private final String candidateName;
    private final Integer rating;
    private final FeedbackRecommendation recommendation;

    public FeedbackSubmittedEvent(Object source, UUID interviewId, String interviewTitle,
                                   String interviewerName, String interviewerEmail,
                                   String candidateName, Integer rating,
                                   FeedbackRecommendation recommendation) {
        super(source);
        this.interviewId = interviewId;
        this.interviewTitle = interviewTitle;
        this.interviewerName = interviewerName;
        this.interviewerEmail = interviewerEmail;
        this.candidateName = candidateName;
        this.rating = rating;
        this.recommendation = recommendation;
    }
}


package com.interview_platform_backend.interview_platform_backend.candidate.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private UUID id;
    private UUID interviewId;
    private UUID interviewerId;
    private String interviewerName;
    private String interviewerEmail;
    private Integer rating;
    private FeedbackRecommendation recommendation;
    private String strengths;
    private String weaknesses;
    private String comments;
    private Instant submittedAt;
}


package com.interview_platform_backend.interview_platform_backend.dashboard.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerDashboard {

    private UUID interviewerId;
    private String interviewerName;

    private Long totalAssignedInterviews;
    private Long completedInterviews;
    private Long upcomingInterviews;
    private Long pendingFeedback;
    private Double averageRatingGiven;

    private List<UpcomingInterview> nextInterviews;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingInterview {
        private UUID interviewId;
        private String title;
        private String candidateName;
        private Instant startTime;
        private Instant endTime;
        private String status;
    }
}


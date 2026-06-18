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
public class CandidateDashboard {

    private UUID candidateId;
    private String candidateName;

    private Long totalInterviews;
    private Long completedInterviews;
    private Long upcomingInterviews;
    private Long cancelledInterviews;

    private List<UpcomingInterview> nextInterviews;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingInterview {
        private UUID interviewId;
        private String title;
        private String interviewerNames;
        private Instant startTime;
        private Instant endTime;
        private String status;
        private String meetingLink;
    }
}


package com.interview_platform_backend.interview_platform_backend.dashboard.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    // Counts
    private Long totalInterviews;
    private Long totalUsers;
    private Long totalQuestions;

    // Interview breakdown by status
    private Map<String, Long> interviewsByStatus;

    // Upcoming interviews (next 7 days)
    private Long upcomingInterviews;

    // Today's interviews
    private Long todayInterviews;

    // Feedback stats
    private Long totalFeedbackSubmitted;
    private Double averageRating;

    // Recent activity
    private List<RecentActivity> recentActivities;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type;
        private String description;
        private String timestamp;
    }
}


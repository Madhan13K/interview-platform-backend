package com.interview_platform_backend.interview_platform_backend.report.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsReport {

    // Overview
    private long totalInterviews;
    private long completedInterviews;
    private long cancelledInterviews;
    private long totalCandidates;
    private long totalJobPositions;
    private long openJobPositions;

    // Conversion rates
    private ConversionMetrics conversionMetrics;

    // Time-to-hire metrics
    private TimeToHireMetrics timeToHireMetrics;

    // Interviewer performance
    private List<InterviewerPerformance> interviewerPerformances;

    // Breakdown data
    private Map<String, Long> interviewsByStatus;
    private Map<String, Long> interviewsByType;
    private Map<String, Long> interviewsByMonth;
    private Map<String, Long> candidatesByRecommendation;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversionMetrics {
        private long totalCandidatesScreened;
        private long passedScreening;
        private long passedTechnical;
        private long offersExtended;
        private long hired;
        private double screeningToTechnicalRate;
        private double technicalToOfferRate;
        private double offerAcceptanceRate;
        private double overallConversionRate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeToHireMetrics {
        private double averageDaysToHire;
        private double medianDaysToHire;
        private double averageDaysPerStage;
        private double averageInterviewsPerCandidate;
        private int fastestHireDays;
        private int slowestHireDays;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InterviewerPerformance {
        private String interviewerId;
        private String interviewerName;
        private long totalInterviewsConducted;
        private long feedbackSubmitted;
        private double feedbackSubmissionRate;
        private double averageRatingGiven;
        private long hireRecommendations;
        private long noHireRecommendations;
        private double averageInterviewDurationMinutes;
    }
}


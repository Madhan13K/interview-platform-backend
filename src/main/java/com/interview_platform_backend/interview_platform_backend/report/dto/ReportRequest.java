package com.interview_platform_backend.interview_platform_backend.report.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {

    private ReportType reportType;
    private Instant fromDate;
    private Instant toDate;
    private UUID jobPositionId;
    private UUID interviewerId;
    private InterviewStatus statusFilter;
    private String department;

    public enum ReportType {
        OVERALL_ANALYTICS,
        INTERVIEWER_PERFORMANCE,
        JOB_POSITION_SUMMARY,
        CONVERSION_FUNNEL,
        TIME_TO_HIRE
    }
}


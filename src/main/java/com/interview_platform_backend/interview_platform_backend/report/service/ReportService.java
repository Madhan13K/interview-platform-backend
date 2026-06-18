package com.interview_platform_backend.interview_platform_backend.report.service;

import com.interview_platform_backend.interview_platform_backend.report.dto.AnalyticsReport;
import com.interview_platform_backend.interview_platform_backend.report.dto.ReportRequest;

import java.util.UUID;

public interface ReportService {

    AnalyticsReport generateAnalyticsReport(ReportRequest request);

    AnalyticsReport getInterviewerPerformanceReport(UUID interviewerId);

    byte[] generatePdfReport(ReportRequest request);

    byte[] generateInterviewerPdfReport(UUID interviewerId);

    byte[] generateJobPositionPdfReport(UUID jobPositionId);
}


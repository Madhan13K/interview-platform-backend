package com.interview_platform_backend.interview_platform_backend.report.controller;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.report.dto.AnalyticsReport;
import com.interview_platform_backend.interview_platform_backend.report.dto.ReportRequest;
import com.interview_platform_backend.interview_platform_backend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports & Analytics", description = "Generate PDF reports, analytics, conversion rates, time-to-hire metrics")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ==================== Analytics (JSON) ====================

    @Operation(summary = "Get overall analytics report")
    @ApiResponse(responseCode = "200", description = "Analytics report generated")
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<AnalyticsReport> getAnalyticsReport(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(required = false) InterviewStatus statusFilter,
            @RequestParam(required = false) String department) {

        ReportRequest request = ReportRequest.builder()
                .reportType(ReportRequest.ReportType.OVERALL_ANALYTICS)
                .fromDate(fromDate)
                .toDate(toDate)
                .statusFilter(statusFilter)
                .department(department)
                .build();

        return ResponseEntity.ok(reportService.generateAnalyticsReport(request));
    }

    @Operation(summary = "Get interviewer performance analytics")
    @ApiResponse(responseCode = "200", description = "Interviewer performance report")
    @GetMapping("/analytics/interviewer/{interviewerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<AnalyticsReport> getInterviewerPerformance(@PathVariable UUID interviewerId) {
        return ResponseEntity.ok(reportService.getInterviewerPerformanceReport(interviewerId));
    }

    // ==================== PDF Reports ====================

    @Operation(summary = "Generate overall analytics PDF report")
    @ApiResponse(responseCode = "200", description = "PDF report generated")
    @GetMapping("/pdf/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<byte[]> generateAnalyticsPdf(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(required = false) InterviewStatus statusFilter) {

        ReportRequest request = ReportRequest.builder()
                .reportType(ReportRequest.ReportType.OVERALL_ANALYTICS)
                .fromDate(fromDate)
                .toDate(toDate)
                .statusFilter(statusFilter)
                .build();

        byte[] pdf = reportService.generatePdfReport(request);
        return buildPdfResponse(pdf, "analytics_report.pdf");
    }

    @Operation(summary = "Generate interviewer performance PDF report")
    @ApiResponse(responseCode = "200", description = "PDF report generated")
    @GetMapping("/pdf/interviewer/{interviewerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<byte[]> generateInterviewerPdf(@PathVariable UUID interviewerId) {
        byte[] pdf = reportService.generateInterviewerPdfReport(interviewerId);
        return buildPdfResponse(pdf, "interviewer_performance_" + interviewerId + ".pdf");
    }

    @Operation(summary = "Generate job position PDF report")
    @ApiResponse(responseCode = "200", description = "PDF report generated")
    @GetMapping("/pdf/job-position/{jobPositionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<byte[]> generateJobPositionPdf(@PathVariable UUID jobPositionId) {
        byte[] pdf = reportService.generateJobPositionPdfReport(jobPositionId);
        return buildPdfResponse(pdf, "job_position_report_" + jobPositionId + ".pdf");
    }

    // ==================== Specific Metrics (JSON) ====================

    @Operation(summary = "Get conversion funnel metrics")
    @ApiResponse(responseCode = "200", description = "Conversion metrics")
    @GetMapping("/metrics/conversion")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<AnalyticsReport.ConversionMetrics> getConversionMetrics(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {

        ReportRequest request = ReportRequest.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        AnalyticsReport report = reportService.generateAnalyticsReport(request);
        return ResponseEntity.ok(report.getConversionMetrics());
    }

    @Operation(summary = "Get time-to-hire metrics")
    @ApiResponse(responseCode = "200", description = "Time-to-hire metrics")
    @GetMapping("/metrics/time-to-hire")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<AnalyticsReport.TimeToHireMetrics> getTimeToHireMetrics(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {

        ReportRequest request = ReportRequest.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        AnalyticsReport report = reportService.generateAnalyticsReport(request);
        return ResponseEntity.ok(report.getTimeToHireMetrics());
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}


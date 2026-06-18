package com.interview_platform_backend.interview_platform_backend.report.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.*;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewFeedbackRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewInterviewerRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPositionStatus;
import com.interview_platform_backend.interview_platform_backend.jobposition.repository.JobPositionRepository;
import com.interview_platform_backend.interview_platform_backend.report.dto.AnalyticsReport;
import com.interview_platform_backend.interview_platform_backend.report.dto.ReportRequest;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final InterviewInterviewerRepository interviewerRepository;
    private final UserRepository userRepository;
    private final JobPositionRepository jobPositionRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    public ReportServiceImpl(InterviewRepository interviewRepository,
                              InterviewFeedbackRepository feedbackRepository,
                              InterviewInterviewerRepository interviewerRepository,
                              UserRepository userRepository,
                              JobPositionRepository jobPositionRepository) {
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.interviewerRepository = interviewerRepository;
        this.userRepository = userRepository;
        this.jobPositionRepository = jobPositionRepository;
    }

    @Override
    public AnalyticsReport generateAnalyticsReport(ReportRequest request) {
        List<Interview> interviews = getFilteredInterviews(request);
        List<InterviewFeedBack> allFeedback = feedbackRepository.findAll();

        long totalInterviews = interviews.size();
        long completed = interviews.stream().filter(i -> i.getStatus() == InterviewStatus.COMPLETED).count();
        long cancelled = interviews.stream().filter(i -> i.getStatus() == InterviewStatus.CANCELLED).count();

        long totalCandidates = interviews.stream()
                .map(i -> i.getCandidate().getId()).collect(Collectors.toSet()).size();

        long totalJobPositions = jobPositionRepository.count();
        long openJobPositions = jobPositionRepository.countByStatus(JobPositionStatus.OPEN);

        // Interviews by status
        Map<String, Long> byStatus = interviews.stream()
                .collect(Collectors.groupingBy(i -> i.getStatus().name(), Collectors.counting()));

        // Interviews by type
        Map<String, Long> byType = interviews.stream()
                .collect(Collectors.groupingBy(i -> i.getType().name(), Collectors.counting()));

        // Interviews by month
        Map<String, Long> byMonth = interviews.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getCreatedAt().atZone(ZoneOffset.UTC).getMonth().name() + " "
                                + i.getCreatedAt().atZone(ZoneOffset.UTC).getYear(),
                        Collectors.counting()));

        // Candidates by recommendation
        Map<String, Long> byRecommendation = allFeedback.stream()
                .collect(Collectors.groupingBy(f -> f.getRecommendation().name(), Collectors.counting()));

        // Conversion metrics
        AnalyticsReport.ConversionMetrics conversion = calculateConversionMetrics(interviews, allFeedback);

        // Time-to-hire
        AnalyticsReport.TimeToHireMetrics timeToHire = calculateTimeToHireMetrics(interviews);

        // Interviewer performance
        List<AnalyticsReport.InterviewerPerformance> performanceList = calculateInterviewerPerformance(interviews, allFeedback);

        return AnalyticsReport.builder()
                .totalInterviews(totalInterviews)
                .completedInterviews(completed)
                .cancelledInterviews(cancelled)
                .totalCandidates(totalCandidates)
                .totalJobPositions(totalJobPositions)
                .openJobPositions(openJobPositions)
                .conversionMetrics(conversion)
                .timeToHireMetrics(timeToHire)
                .interviewerPerformances(performanceList)
                .interviewsByStatus(byStatus)
                .interviewsByType(byType)
                .interviewsByMonth(byMonth)
                .candidatesByRecommendation(byRecommendation)
                .build();
    }

    @Override
    public AnalyticsReport getInterviewerPerformanceReport(UUID interviewerId) {
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        List<Interview> interviews = interviewRepository.findByInterviewerId(interviewerId);
        List<InterviewFeedBack> feedback = feedbackRepository.findByInterviewer(interviewer);

        AnalyticsReport.InterviewerPerformance performance = buildInterviewerPerformance(
                interviewer, interviews, feedback);

        return AnalyticsReport.builder()
                .totalInterviews((long) interviews.size())
                .completedInterviews(interviews.stream().filter(i -> i.getStatus() == InterviewStatus.COMPLETED).count())
                .interviewerPerformances(List.of(performance))
                .build();
    }

    @Override
    public byte[] generatePdfReport(ReportRequest request) {
        AnalyticsReport report = generateAnalyticsReport(request);
        return buildAnalyticsPdf(report, "Interview Platform - Analytics Report");
    }

    @Override
    public byte[] generateInterviewerPdfReport(UUID interviewerId) {
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        AnalyticsReport report = getInterviewerPerformanceReport(interviewerId);
        return buildAnalyticsPdf(report, "Interviewer Performance Report - "
                + interviewer.getFirstName() + " " + interviewer.getLastName());
    }

    @Override
    public byte[] generateJobPositionPdfReport(UUID jobPositionId) {
        JobPosition jp = jobPositionRepository.findById(jobPositionId)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", jobPositionId));

        List<Interview> interviews = jp.getInterviews() != null ? jp.getInterviews() : List.of();
        return buildJobPositionPdf(jp, interviews);
    }

    // ==================== Private Helpers ====================

    private List<Interview> getFilteredInterviews(ReportRequest request) {
        if (request == null) return interviewRepository.findAllWithDetails();

        List<Interview> interviews;
        if (request.getFromDate() != null && request.getToDate() != null) {
            interviews = interviewRepository.findByDateRange(request.getFromDate(), request.getToDate());
        } else {
            interviews = interviewRepository.findAllWithDetails();
        }

        if (request.getStatusFilter() != null) {
            interviews = interviews.stream()
                    .filter(i -> i.getStatus() == request.getStatusFilter())
                    .toList();
        }

        return interviews;
    }

    private AnalyticsReport.ConversionMetrics calculateConversionMetrics(
            List<Interview> interviews, List<InterviewFeedBack> allFeedback) {

        long totalScreened = interviews.stream()
                .filter(i -> i.getType() == InterviewType.SCREENING)
                .map(i -> i.getCandidate().getId()).collect(Collectors.toSet()).size();

        long passedScreening = allFeedback.stream()
                .filter(f -> f.getInterview().getType() == InterviewType.SCREENING)
                .filter(f -> f.getRecommendation() == FeedbackRecommendation.HIRE)
                .map(f -> f.getInterview().getCandidate().getId()).collect(Collectors.toSet()).size();

        long passedTechnical = allFeedback.stream()
                .filter(f -> f.getInterview().getType() == InterviewType.TECHNICAL)
                .filter(f -> f.getRecommendation() == FeedbackRecommendation.HIRE)
                .map(f -> f.getInterview().getCandidate().getId()).collect(Collectors.toSet()).size();

        long hireRecommendations = allFeedback.stream()
                .filter(f -> f.getRecommendation() == FeedbackRecommendation.HIRE)
                .map(f -> f.getInterview().getCandidate().getId()).collect(Collectors.toSet()).size();

        long totalCandidates = interviews.stream()
                .map(i -> i.getCandidate().getId()).collect(Collectors.toSet()).size();

        double screenToTech = totalScreened > 0 ? (double) passedScreening / totalScreened * 100 : 0;
        double techToOffer = passedScreening > 0 ? (double) passedTechnical / passedScreening * 100 : 0;
        double overallConversion = totalCandidates > 0 ? (double) hireRecommendations / totalCandidates * 100 : 0;

        return AnalyticsReport.ConversionMetrics.builder()
                .totalCandidatesScreened(totalScreened)
                .passedScreening(passedScreening)
                .passedTechnical(passedTechnical)
                .offersExtended(hireRecommendations)
                .hired(hireRecommendations)
                .screeningToTechnicalRate(round(screenToTech))
                .technicalToOfferRate(round(techToOffer))
                .offerAcceptanceRate(0)
                .overallConversionRate(round(overallConversion))
                .build();
    }

    private AnalyticsReport.TimeToHireMetrics calculateTimeToHireMetrics(List<Interview> interviews) {
        // Group by candidate, measure time from first to last interview
        Map<UUID, List<Interview>> byCandiate = interviews.stream()
                .collect(Collectors.groupingBy(i -> i.getCandidate().getId()));

        List<Long> daysToHire = new ArrayList<>();
        for (List<Interview> candidateInterviews : byCandiate.values()) {
            if (candidateInterviews.size() < 2) continue;
            Instant first = candidateInterviews.stream().map(Interview::getStartTime).min(Instant::compareTo).orElse(null);
            Instant last = candidateInterviews.stream().map(Interview::getStartTime).max(Instant::compareTo).orElse(null);
            if (first != null && last != null) {
                long days = Duration.between(first, last).toDays();
                if (days > 0) daysToHire.add(days);
            }
        }

        double avgDays = daysToHire.stream().mapToLong(Long::longValue).average().orElse(0);
        double medianDays = 0;
        if (!daysToHire.isEmpty()) {
            Collections.sort(daysToHire);
            medianDays = daysToHire.get(daysToHire.size() / 2);
        }

        double avgInterviewsPerCandidate = byCandiate.isEmpty() ? 0
                : (double) interviews.size() / byCandiate.size();

        int fastest = daysToHire.isEmpty() ? 0 : daysToHire.stream().mapToInt(Long::intValue).min().orElse(0);
        int slowest = daysToHire.isEmpty() ? 0 : daysToHire.stream().mapToInt(Long::intValue).max().orElse(0);

        return AnalyticsReport.TimeToHireMetrics.builder()
                .averageDaysToHire(round(avgDays))
                .medianDaysToHire(round(medianDays))
                .averageDaysPerStage(round(avgDays / Math.max(avgInterviewsPerCandidate, 1)))
                .averageInterviewsPerCandidate(round(avgInterviewsPerCandidate))
                .fastestHireDays(fastest)
                .slowestHireDays(slowest)
                .build();
    }

    private List<AnalyticsReport.InterviewerPerformance> calculateInterviewerPerformance(
            List<Interview> interviews, List<InterviewFeedBack> allFeedback) {

        // Get unique interviewers from feedback
        Map<UUID, List<InterviewFeedBack>> feedbackByInterviewer = allFeedback.stream()
                .collect(Collectors.groupingBy(f -> f.getInterviewer().getId()));

        return feedbackByInterviewer.entrySet().stream()
                .map(entry -> {
                    User interviewer = entry.getValue().get(0).getInterviewer();
                    List<InterviewFeedBack> feedback = entry.getValue();
                    List<Interview> interviewerInterviews = interviewRepository.findByInterviewerId(entry.getKey());
                    return buildInterviewerPerformance(interviewer, interviewerInterviews, feedback);
                })
                .sorted((a, b) -> Long.compare(b.getTotalInterviewsConducted(), a.getTotalInterviewsConducted()))
                .limit(20)
                .toList();
    }

    private AnalyticsReport.InterviewerPerformance buildInterviewerPerformance(
            User interviewer, List<Interview> interviews, List<InterviewFeedBack> feedback) {

        long totalConducted = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED).count();
        long feedbackCount = feedback.size();
        double submissionRate = totalConducted > 0 ? (double) feedbackCount / totalConducted * 100 : 0;
        double avgRating = feedback.stream().mapToInt(InterviewFeedBack::getRating).average().orElse(0);
        long hireRecs = feedback.stream()
                .filter(f -> f.getRecommendation() == FeedbackRecommendation.HIRE).count();
        long noHireRecs = feedback.stream()
                .filter(f -> f.getRecommendation() == FeedbackRecommendation.NO_HIRE
                        || f.getRecommendation() == FeedbackRecommendation.STRONG_NO_HIRE).count();

        double avgDuration = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                .mapToLong(i -> Duration.between(i.getStartTime(), i.getEndTime()).toMinutes())
                .average().orElse(0);

        return AnalyticsReport.InterviewerPerformance.builder()
                .interviewerId(interviewer.getId().toString())
                .interviewerName(interviewer.getFirstName() + " " + interviewer.getLastName())
                .totalInterviewsConducted(totalConducted)
                .feedbackSubmitted(feedbackCount)
                .feedbackSubmissionRate(round(submissionRate))
                .averageRatingGiven(round(avgRating))
                .hireRecommendations(hireRecs)
                .noHireRecommendations(noHireRecs)
                .averageInterviewDurationMinutes(round(avgDuration))
                .build();
    }

    // ==================== PDF Generation ====================

    private byte[] buildAnalyticsPdf(AnalyticsReport report, String title) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(33, 37, 41));
            Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(52, 58, 64));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

            // Title
            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setAlignment(Element.ALIGN_CENTER);
            titleParagraph.setSpacingAfter(5);
            document.add(titleParagraph);

            // Generation date
            Paragraph dateParagraph = new Paragraph("Generated: " + DATE_FMT.format(Instant.now()), smallFont);
            dateParagraph.setAlignment(Element.ALIGN_CENTER);
            dateParagraph.setSpacingAfter(20);
            document.add(dateParagraph);

            // Overview Section
            document.add(new Paragraph("Overview", headerFont));
            document.add(new Paragraph(" ", normalFont));

            PdfPTable overviewTable = new PdfPTable(2);
            overviewTable.setWidthPercentage(100);
            addTableRow(overviewTable, "Total Interviews", String.valueOf(report.getTotalInterviews()), normalFont);
            addTableRow(overviewTable, "Completed Interviews", String.valueOf(report.getCompletedInterviews()), normalFont);
            addTableRow(overviewTable, "Cancelled Interviews", String.valueOf(report.getCancelledInterviews()), normalFont);
            addTableRow(overviewTable, "Total Candidates", String.valueOf(report.getTotalCandidates()), normalFont);
            addTableRow(overviewTable, "Total Job Positions", String.valueOf(report.getTotalJobPositions()), normalFont);
            addTableRow(overviewTable, "Open Positions", String.valueOf(report.getOpenJobPositions()), normalFont);
            document.add(overviewTable);
            document.add(new Paragraph(" ", normalFont));

            // Conversion Metrics
            if (report.getConversionMetrics() != null) {
                document.add(new Paragraph("Conversion Metrics", headerFont));
                document.add(new Paragraph(" ", normalFont));

                PdfPTable convTable = new PdfPTable(2);
                convTable.setWidthPercentage(100);
                AnalyticsReport.ConversionMetrics cm = report.getConversionMetrics();
                addTableRow(convTable, "Candidates Screened", String.valueOf(cm.getTotalCandidatesScreened()), normalFont);
                addTableRow(convTable, "Passed Screening", String.valueOf(cm.getPassedScreening()), normalFont);
                addTableRow(convTable, "Passed Technical", String.valueOf(cm.getPassedTechnical()), normalFont);
                addTableRow(convTable, "Screening → Technical Rate", cm.getScreeningToTechnicalRate() + "%", normalFont);
                addTableRow(convTable, "Technical → Offer Rate", cm.getTechnicalToOfferRate() + "%", normalFont);
                addTableRow(convTable, "Overall Conversion Rate", cm.getOverallConversionRate() + "%", normalFont);
                document.add(convTable);
                document.add(new Paragraph(" ", normalFont));
            }

            // Time-to-Hire Metrics
            if (report.getTimeToHireMetrics() != null) {
                document.add(new Paragraph("Time-to-Hire Metrics", headerFont));
                document.add(new Paragraph(" ", normalFont));

                PdfPTable tthTable = new PdfPTable(2);
                tthTable.setWidthPercentage(100);
                AnalyticsReport.TimeToHireMetrics tth = report.getTimeToHireMetrics();
                addTableRow(tthTable, "Average Days to Hire", String.valueOf(tth.getAverageDaysToHire()), normalFont);
                addTableRow(tthTable, "Median Days to Hire", String.valueOf(tth.getMedianDaysToHire()), normalFont);
                addTableRow(tthTable, "Avg Days per Stage", String.valueOf(tth.getAverageDaysPerStage()), normalFont);
                addTableRow(tthTable, "Avg Interviews per Candidate", String.valueOf(tth.getAverageInterviewsPerCandidate()), normalFont);
                addTableRow(tthTable, "Fastest Hire (days)", String.valueOf(tth.getFastestHireDays()), normalFont);
                addTableRow(tthTable, "Slowest Hire (days)", String.valueOf(tth.getSlowestHireDays()), normalFont);
                document.add(tthTable);
                document.add(new Paragraph(" ", normalFont));
            }

            // Interviewer Performance
            if (report.getInterviewerPerformances() != null && !report.getInterviewerPerformances().isEmpty()) {
                document.add(new Paragraph("Interviewer Performance", headerFont));
                document.add(new Paragraph(" ", normalFont));

                PdfPTable perfTable = new PdfPTable(6);
                perfTable.setWidthPercentage(100);
                perfTable.setWidths(new float[]{3, 2, 2, 2, 2, 2});

                addHeaderCell(perfTable, "Interviewer");
                addHeaderCell(perfTable, "Interviews");
                addHeaderCell(perfTable, "Feedback");
                addHeaderCell(perfTable, "Avg Rating");
                addHeaderCell(perfTable, "Hire Recs");
                addHeaderCell(perfTable, "No-Hire");

                for (AnalyticsReport.InterviewerPerformance perf : report.getInterviewerPerformances()) {
                    perfTable.addCell(new Phrase(perf.getInterviewerName(), normalFont));
                    perfTable.addCell(new Phrase(String.valueOf(perf.getTotalInterviewsConducted()), normalFont));
                    perfTable.addCell(new Phrase(String.valueOf(perf.getFeedbackSubmitted()), normalFont));
                    perfTable.addCell(new Phrase(String.valueOf(perf.getAverageRatingGiven()), normalFont));
                    perfTable.addCell(new Phrase(String.valueOf(perf.getHireRecommendations()), normalFont));
                    perfTable.addCell(new Phrase(String.valueOf(perf.getNoHireRecommendations()), normalFont));
                }
                document.add(perfTable);
            }

            // Breakdowns
            if (report.getInterviewsByStatus() != null && !report.getInterviewsByStatus().isEmpty()) {
                document.add(new Paragraph(" ", normalFont));
                document.add(new Paragraph("Interviews by Status", headerFont));
                document.add(new Paragraph(" ", normalFont));

                PdfPTable statusTable = new PdfPTable(2);
                statusTable.setWidthPercentage(60);
                for (Map.Entry<String, Long> entry : report.getInterviewsByStatus().entrySet()) {
                    addTableRow(statusTable, entry.getKey(), String.valueOf(entry.getValue()), normalFont);
                }
                document.add(statusTable);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private byte[] buildJobPositionPdf(JobPosition jp, List<Interview> interviews) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(33, 37, 41));
            Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(52, 58, 64));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

            // Title
            Paragraph titleParagraph = new Paragraph("Job Position Report: " + jp.getTitle(), titleFont);
            titleParagraph.setAlignment(Element.ALIGN_CENTER);
            titleParagraph.setSpacingAfter(5);
            document.add(titleParagraph);

            Paragraph dateParagraph = new Paragraph("Generated: " + DATE_FMT.format(Instant.now()), smallFont);
            dateParagraph.setAlignment(Element.ALIGN_CENTER);
            dateParagraph.setSpacingAfter(20);
            document.add(dateParagraph);

            // Position details
            document.add(new Paragraph("Position Details", headerFont));
            document.add(new Paragraph(" ", normalFont));

            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            addTableRow(detailsTable, "Title", jp.getTitle(), normalFont);
            addTableRow(detailsTable, "Department", jp.getDepartment() != null ? jp.getDepartment() : "N/A", normalFont);
            addTableRow(detailsTable, "Location", jp.getLocation() != null ? jp.getLocation() : "N/A", normalFont);
            addTableRow(detailsTable, "Status", jp.getStatus().name(), normalFont);
            addTableRow(detailsTable, "Employment Type", jp.getEmploymentType().name(), normalFont);
            addTableRow(detailsTable, "Experience Level", jp.getExperienceLevel().name(), normalFont);
            addTableRow(detailsTable, "Openings", String.valueOf(jp.getNumberOfOpenings()), normalFont);
            addTableRow(detailsTable, "Hired", String.valueOf(jp.getNumberHired()), normalFont);
            addTableRow(detailsTable, "Total Interviews", String.valueOf(interviews.size()), normalFont);
            document.add(detailsTable);
            document.add(new Paragraph(" ", normalFont));

            // Interview list
            if (!interviews.isEmpty()) {
                document.add(new Paragraph("Linked Interviews", headerFont));
                document.add(new Paragraph(" ", normalFont));

                PdfPTable intTable = new PdfPTable(5);
                intTable.setWidthPercentage(100);
                intTable.setWidths(new float[]{3, 3, 2, 2, 2});

                addHeaderCell(intTable, "Title");
                addHeaderCell(intTable, "Candidate");
                addHeaderCell(intTable, "Status");
                addHeaderCell(intTable, "Type");
                addHeaderCell(intTable, "Date");

                for (Interview i : interviews) {
                    intTable.addCell(new Phrase(i.getTitle(), normalFont));
                    intTable.addCell(new Phrase(
                            i.getCandidate().getFirstName() + " " + i.getCandidate().getLastName(), normalFont));
                    intTable.addCell(new Phrase(i.getStatus().name(), normalFont));
                    intTable.addCell(new Phrase(i.getType().name(), normalFont));
                    intTable.addCell(new Phrase(DATE_FMT.format(i.getStartTime()), normalFont));
                }
                document.add(intTable);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate job position PDF report: " + e.getMessage(), e);
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(0);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new Color(248, 249, 250));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(0);
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(new Color(52, 58, 64));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}


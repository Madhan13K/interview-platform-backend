package com.interview_platform_backend.interview_platform_backend.bulk.service;

import com.interview_platform_backend.interview_platform_backend.audit.AuditAction;
import com.interview_platform_backend.interview_platform_backend.audit.AuditService;
import com.interview_platform_backend.interview_platform_backend.bulk.dto.*;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.CreateInterviewRequest;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.service.InterviewService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BulkOperationServiceImpl implements BulkOperationService {

    private static final Logger log = LoggerFactory.getLogger(BulkOperationServiceImpl.class);
    private static final int MAX_BULK_SIZE = 100;

    private final InterviewService interviewService;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final AuditService auditService;

    public BulkOperationServiceImpl(InterviewService interviewService,
                                     InterviewRepository interviewRepository,
                                     UserRepository userRepository,
                                     EmailNotificationService emailNotificationService,
                                     AuditService auditService) {
        this.interviewService = interviewService;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
        this.auditService = auditService;
    }

    @Override
    public BulkOperationResponse<InterviewResponse> bulkScheduleInterviews(
            BulkScheduleInterviewsRequest request, UUID scheduledByUserId) {

        if (request.getInterviews().size() > MAX_BULK_SIZE) {
            throw new BadRequestException("Cannot schedule more than " + MAX_BULK_SIZE + " interviews at once");
        }

        List<InterviewResponse> successResults = new ArrayList<>();
        List<BulkOperationResponse.BulkError> errors = new ArrayList<>();

        for (int i = 0; i < request.getInterviews().size(); i++) {
            BulkScheduleInterviewsRequest.BulkInterviewItem item = request.getInterviews().get(i);
            try {
                CreateInterviewRequest createRequest = CreateInterviewRequest.builder()
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .candidateId(item.getCandidateId())
                        .startTime(item.getStartTime())
                        .endTime(item.getEndTime())
                        .timeZone(item.getTimeZone())
                        .type(item.getType())
                        .mode(item.getMode())
                        .meetingLink(item.getMeetingLink())
                        .location(item.getLocation())
                        .interviewerIds(item.getInterviewerIds())
                        .build();

                InterviewResponse response = interviewService.createInterview(createRequest, scheduledByUserId);
                successResults.add(response);
            } catch (Exception e) {
                log.warn("Bulk schedule - failed item {}: {}", i, e.getMessage());
                errors.add(BulkOperationResponse.BulkError.builder()
                        .index(i)
                        .identifier(item.getTitle() + " - " + item.getCandidateId())
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        auditService.log("BulkOperation", null, AuditAction.BULK_SCHEDULE,
                String.format("Bulk scheduled %d interviews, %d succeeded, %d failed",
                        request.getInterviews().size(), successResults.size(), errors.size()));

        return BulkOperationResponse.<InterviewResponse>builder()
                .totalRequested(request.getInterviews().size())
                .successCount(successResults.size())
                .failureCount(errors.size())
                .successResults(successResults)
                .errors(errors)
                .build();
    }

    @Override
    public BulkOperationResponse<BulkInviteResult> bulkInviteCandidates(BulkInviteCandidatesRequest request) {
        if (request.getCandidates().size() > MAX_BULK_SIZE) {
            throw new BadRequestException("Cannot invite more than " + MAX_BULK_SIZE + " candidates at once");
        }

        List<BulkInviteResult> successResults = new ArrayList<>();
        List<BulkOperationResponse.BulkError> errors = new ArrayList<>();

        for (int i = 0; i < request.getCandidates().size(); i++) {
            BulkInviteCandidatesRequest.CandidateInvite invite = request.getCandidates().get(i);
            try {
                String email = invite.getEmail();
                String candidateName = invite.getFirstName() + " " + invite.getLastName();

                // If candidateId is provided, look up their details
                if (invite.getCandidateId() != null) {
                    User candidate = userRepository.findById(invite.getCandidateId()).orElse(null);
                    if (candidate != null) {
                        email = candidate.getEmail();
                        candidateName = candidate.getFirstName() + " " + candidate.getLastName();
                    }
                }

                if (email == null || email.isBlank()) {
                    throw new BadRequestException("Email is required for candidate at index " + i);
                }

                String subject = "Interview Invitation";
                String body = buildInvitationEmail(candidateName, invite.getCustomMessage());
                emailNotificationService.sendEmail(email, subject, body);

                successResults.add(BulkInviteResult.builder()
                        .email(email)
                        .candidateName(candidateName)
                        .sent(true)
                        .message("Invitation sent successfully")
                        .build());

            } catch (Exception e) {
                log.warn("Bulk invite - failed item {}: {}", i, e.getMessage());
                errors.add(BulkOperationResponse.BulkError.builder()
                        .index(i)
                        .identifier(invite.getEmail() != null ? invite.getEmail() : String.valueOf(invite.getCandidateId()))
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        auditService.log("BulkOperation", request.getInterviewId(), AuditAction.BULK_INVITE,
                String.format("Bulk invited %d candidates, %d succeeded, %d failed",
                        request.getCandidates().size(), successResults.size(), errors.size()));

        return BulkOperationResponse.<BulkInviteResult>builder()
                .totalRequested(request.getCandidates().size())
                .successCount(successResults.size())
                .failureCount(errors.size())
                .successResults(successResults)
                .errors(errors)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] bulkExportData(BulkExportRequest request) {
        if (request.getExportType() == null) {
            throw new BadRequestException("Export type is required");
        }

        return switch (request.getExportType()) {
            case INTERVIEWS -> exportInterviews(request);
            case CANDIDATES -> exportCandidates(request);
            case FEEDBACK -> exportFeedback(request);
            case SCORECARDS -> exportScorecards(request);
        };
    }

    private byte[] exportInterviews(BulkExportRequest request) {
        List<Interview> interviews;
        if (request.getStatusFilter() != null) {
            interviews = interviewRepository.findByStatus(request.getStatusFilter());
        } else if (request.getFromDate() != null && request.getToDate() != null) {
            interviews = interviewRepository.findByDateRange(request.getFromDate(), request.getToDate());
        } else {
            interviews = interviewRepository.findAllWithDetails();
        }

        if (request.getFormat() == BulkExportRequest.ExportFormat.JSON) {
            return exportInterviewsAsJson(interviews);
        }
        return exportInterviewsAsCsv(interviews);
    }

    private byte[] exportInterviewsAsCsv(List<Interview> interviews) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("ID", "Title", "Candidate", "Candidate Email", "Status", "Type", "Mode",
                            "Start Time", "End Time", "Scheduled By", "Meeting Link", "Location", "Created At")
                    .build());

            for (Interview interview : interviews) {
                csvPrinter.printRecord(
                        interview.getId(),
                        interview.getTitle(),
                        interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName(),
                        interview.getCandidate().getEmail(),
                        interview.getStatus(),
                        interview.getType(),
                        interview.getMode(),
                        interview.getStartTime(),
                        interview.getEndTime(),
                        interview.getScheduledBy().getFirstName() + " " + interview.getScheduledBy().getLastName(),
                        interview.getMeetingLink(),
                        interview.getLocation(),
                        interview.getCreatedAt()
                );
            }

            csvPrinter.flush();
            csvPrinter.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to export interviews: " + e.getMessage());
        }
    }

    private byte[] exportInterviewsAsJson(List<Interview> interviews) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
            writer.println("[");

            for (int i = 0; i < interviews.size(); i++) {
                Interview interview = interviews.get(i);
                writer.printf("""
                  {
                    "id": "%s",
                    "title": "%s",
                    "candidate": "%s",
                    "candidateEmail": "%s",
                    "status": "%s",
                    "type": "%s",
                    "mode": "%s",
                    "startTime": "%s",
                    "endTime": "%s",
                    "scheduledBy": "%s",
                    "meetingLink": "%s",
                    "location": "%s",
                    "createdAt": "%s"
                  }%s
                """,
                        interview.getId(),
                        escapeJson(interview.getTitle()),
                        escapeJson(interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName()),
                        escapeJson(interview.getCandidate().getEmail()),
                        interview.getStatus(),
                        interview.getType(),
                        interview.getMode(),
                        interview.getStartTime(),
                        interview.getEndTime(),
                        escapeJson(interview.getScheduledBy().getFirstName() + " " + interview.getScheduledBy().getLastName()),
                        escapeJson(interview.getMeetingLink() != null ? interview.getMeetingLink() : ""),
                        escapeJson(interview.getLocation() != null ? interview.getLocation() : ""),
                        interview.getCreatedAt(),
                        i < interviews.size() - 1 ? "," : ""
                );
            }

            writer.println("]");
            writer.flush();
            writer.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to export interviews as JSON: " + e.getMessage());
        }
    }

    private byte[] exportCandidates(BulkExportRequest request) {
        List<User> candidates = userRepository.findAll();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("ID", "First Name", "Last Name", "Email", "Phone", "Status", "Created At")
                    .build());

            for (User user : candidates) {
                csvPrinter.printRecord(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getStatus(),
                        user.getCreatedAt()
                );
            }

            csvPrinter.flush();
            csvPrinter.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to export candidates: " + e.getMessage());
        }
    }

    private byte[] exportFeedback(BulkExportRequest request) {
        // Export all interview feedback
        List<Interview> interviews = interviewRepository.findAllWithDetails();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("Interview ID", "Interview Title", "Candidate", "Status", "Interviewer Count")
                    .build());

            for (Interview interview : interviews) {
                csvPrinter.printRecord(
                        interview.getId(),
                        interview.getTitle(),
                        interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName(),
                        interview.getStatus(),
                        interview.getInterviewers() != null ? interview.getInterviewers().size() : 0
                );
            }

            csvPrinter.flush();
            csvPrinter.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to export feedback: " + e.getMessage());
        }
    }

    private byte[] exportScorecards(BulkExportRequest request) {
        // Placeholder for scorecard export
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("Interview ID", "Interview Title", "Candidate", "Status")
                    .build());

            List<Interview> interviews = interviewRepository.findAllWithDetails();
            for (Interview interview : interviews) {
                csvPrinter.printRecord(
                        interview.getId(),
                        interview.getTitle(),
                        interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName(),
                        interview.getStatus()
                );
            }

            csvPrinter.flush();
            csvPrinter.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to export scorecards: " + e.getMessage());
        }
    }

    private String buildInvitationEmail(String candidateName, String customMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(candidateName).append(",\n\n");
        sb.append("You have been invited to participate in an interview process.\n\n");
        if (customMessage != null && !customMessage.isBlank()) {
            sb.append("Message from the recruiter:\n");
            sb.append(customMessage).append("\n\n");
        }
        sb.append("Please log in to the interview platform to view your scheduled interviews and prepare accordingly.\n\n");
        sb.append("Best regards,\n");
        sb.append("Interview Platform Team");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}



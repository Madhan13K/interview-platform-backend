package com.interview_platform_backend.interview_platform_backend.exportimport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewFeedBack;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewFeedbackRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.document.service.S3StorageService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobFormat;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobStatus;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobType;
import com.interview_platform_backend.interview_platform_backend.exportimport.repository.ExportImportJobRepository;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.Question;
import com.interview_platform_backend.interview_platform_backend.questionbank.repository.QuestionRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final ExportImportJobRepository jobRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    public ExportService(ExportImportJobRepository jobRepository,
                         InterviewRepository interviewRepository,
                         InterviewFeedbackRepository feedbackRepository,
                         QuestionRepository questionRepository,
                         UserRepository userRepository,
                         S3StorageService s3StorageService) {
        this.jobRepository = jobRepository;
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.s3StorageService = s3StorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public ExportImportJob createExportJob(String entityType, JobFormat format, Map<String, String> filters, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found: " + userId));

        ExportImportJob job = ExportImportJob.builder()
                .organizationId(userId) // Using userId as org placeholder
                .user(user)
                .type(JobType.EXPORT)
                .format(format)
                .status(JobStatus.PENDING)
                .entityType(entityType)
                .filters(filters != null ? serializeFilters(filters) : null)
                .totalRecords(0)
                .processedRecords(0)
                .build();

        return jobRepository.save(job);
    }

    @Async
    public void exportInterviews(UUID jobId, Map<String, String> filters, JobFormat format) {
        ExportImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            List<Interview> interviews = interviewRepository.findAllWithDetails();

            // Apply filters
            if (filters != null) {
                if (filters.containsKey("status")) {
                    String statusFilter = filters.get("status");
                    interviews = interviews.stream()
                            .filter(i -> i.getStatus().name().equalsIgnoreCase(statusFilter))
                            .collect(Collectors.toList());
                }
                if (filters.containsKey("type")) {
                    String typeFilter = filters.get("type");
                    interviews = interviews.stream()
                            .filter(i -> i.getType().name().equalsIgnoreCase(typeFilter))
                            .collect(Collectors.toList());
                }
            }

            job.setTotalRecords(interviews.size());
            jobRepository.save(job);

            byte[] content;
            String fileExtension;

            if (format == JobFormat.JSON) {
                content = generateInterviewsJson(interviews);
                fileExtension = "json";
            } else {
                // Default to CSV (Excel support TODO: add Apache POI dependency)
                content = generateInterviewsCsv(interviews);
                fileExtension = "csv";
            }

            String fileName = "interviews_export_" + Instant.now().toEpochMilli() + "." + fileExtension;
            String s3Key = s3StorageService.generateS3Key("exports", job.getUser().getId(), fileName);

            // Upload to S3 using presigned URL approach or direct upload
            uploadBytes(content, s3Key, format == JobFormat.JSON ? "application/json" : "text/csv");

            job.setFileName(fileName);
            job.setS3Key(s3Key);
            job.setProcessedRecords(interviews.size());
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            log.info("Export job {} completed. {} records exported.", jobId, interviews.size());

        } catch (Exception e) {
            log.error("Export job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    @Async
    public void exportCandidates(UUID jobId, Map<String, String> filters, JobFormat format) {
        ExportImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            // Export candidates (users with candidate role / who have interviews as candidates)
            List<Interview> interviews = interviewRepository.findAllWithDetails();
            Set<User> candidates = interviews.stream()
                    .map(Interview::getCandidate)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            job.setTotalRecords(candidates.size());
            jobRepository.save(job);

            byte[] content;
            String fileExtension;

            if (format == JobFormat.JSON) {
                content = generateCandidatesJson(candidates);
                fileExtension = "json";
            } else {
                content = generateCandidatesCsv(candidates);
                fileExtension = "csv";
            }

            String fileName = "candidates_export_" + Instant.now().toEpochMilli() + "." + fileExtension;
            String s3Key = s3StorageService.generateS3Key("exports", job.getUser().getId(), fileName);

            uploadBytes(content, s3Key, format == JobFormat.JSON ? "application/json" : "text/csv");

            job.setFileName(fileName);
            job.setS3Key(s3Key);
            job.setProcessedRecords(candidates.size());
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            log.info("Export job {} completed. {} candidate records exported.", jobId, candidates.size());

        } catch (Exception e) {
            log.error("Export job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    @Async
    public void exportFeedback(UUID jobId, Map<String, String> filters, JobFormat format) {
        ExportImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            List<InterviewFeedBack> feedbacks = feedbackRepository.findAll();

            job.setTotalRecords(feedbacks.size());
            jobRepository.save(job);

            byte[] content;
            String fileExtension;

            if (format == JobFormat.JSON) {
                content = generateFeedbackJson(feedbacks);
                fileExtension = "json";
            } else {
                content = generateFeedbackCsv(feedbacks);
                fileExtension = "csv";
            }

            String fileName = "feedback_export_" + Instant.now().toEpochMilli() + "." + fileExtension;
            String s3Key = s3StorageService.generateS3Key("exports", job.getUser().getId(), fileName);

            uploadBytes(content, s3Key, format == JobFormat.JSON ? "application/json" : "text/csv");

            job.setFileName(fileName);
            job.setS3Key(s3Key);
            job.setProcessedRecords(feedbacks.size());
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            log.info("Export job {} completed. {} feedback records exported.", jobId, feedbacks.size());

        } catch (Exception e) {
            log.error("Export job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    @Async
    public void exportQuestions(UUID jobId, Map<String, String> filters, JobFormat format) {
        ExportImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            List<Question> questions = questionRepository.findAll();

            // Apply filters
            if (filters != null) {
                if (filters.containsKey("difficulty")) {
                    String difficultyFilter = filters.get("difficulty");
                    questions = questions.stream()
                            .filter(q -> q.getDifficulty().name().equalsIgnoreCase(difficultyFilter))
                            .collect(Collectors.toList());
                }
                if (filters.containsKey("type")) {
                    String typeFilter = filters.get("type");
                    questions = questions.stream()
                            .filter(q -> q.getType().name().equalsIgnoreCase(typeFilter))
                            .collect(Collectors.toList());
                }
            }

            job.setTotalRecords(questions.size());
            jobRepository.save(job);

            byte[] content;
            String fileExtension;

            if (format == JobFormat.JSON) {
                content = generateQuestionsJson(questions);
                fileExtension = "json";
            } else {
                content = generateQuestionsCsv(questions);
                fileExtension = "csv";
            }

            String fileName = "questions_export_" + Instant.now().toEpochMilli() + "." + fileExtension;
            String s3Key = s3StorageService.generateS3Key("exports", job.getUser().getId(), fileName);

            uploadBytes(content, s3Key, format == JobFormat.JSON ? "application/json" : "text/csv");

            job.setFileName(fileName);
            job.setS3Key(s3Key);
            job.setProcessedRecords(questions.size());
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            log.info("Export job {} completed. {} question records exported.", jobId, questions.size());

        } catch (Exception e) {
            log.error("Export job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    // ==================== CSV Generators ====================

    private byte[] generateInterviewsCsv(List<Interview> interviews) throws IOException {
        String[] headers = {"id", "title", "status", "type", "mode", "candidateId", "candidateName",
                "scheduledById", "startTime", "endTime", "timeZone", "meetingLink", "location", "createdAt"};

        StringWriter writer = new StringWriter();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (Interview interview : interviews) {
                printer.printRecord(
                        interview.getId(),
                        interview.getTitle(),
                        interview.getStatus(),
                        interview.getType(),
                        interview.getMode(),
                        interview.getCandidate() != null ? interview.getCandidate().getId() : "",
                        interview.getCandidate() != null ?
                                interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName() : "",
                        interview.getScheduledBy() != null ? interview.getScheduledBy().getId() : "",
                        interview.getStartTime(),
                        interview.getEndTime(),
                        interview.getTimeZone(),
                        interview.getMeetingLink() != null ? interview.getMeetingLink() : "",
                        interview.getLocation() != null ? interview.getLocation() : "",
                        interview.getCreatedAt()
                );
            }
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateCandidatesCsv(Set<User> candidates) throws IOException {
        String[] headers = {"id", "firstName", "lastName", "email", "phoneNumber", "status", "createdAt"};

        StringWriter writer = new StringWriter();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (User candidate : candidates) {
                printer.printRecord(
                        candidate.getId(),
                        candidate.getFirstName(),
                        candidate.getLastName(),
                        candidate.getEmail(),
                        candidate.getPhoneNumber() != null ? candidate.getPhoneNumber() : "",
                        candidate.getStatus() != null ? candidate.getStatus() : "",
                        candidate.getCreatedAt()
                );
            }
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateFeedbackCsv(List<InterviewFeedBack> feedbacks) throws IOException {
        String[] headers = {"id", "interviewId", "interviewerId", "rating", "recommendation",
                "strengths", "weaknesses", "comments", "submittedAt"};

        StringWriter writer = new StringWriter();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (InterviewFeedBack feedback : feedbacks) {
                printer.printRecord(
                        feedback.getId(),
                        feedback.getInterview() != null ? feedback.getInterview().getId() : "",
                        feedback.getInterviewer() != null ? feedback.getInterviewer().getId() : "",
                        feedback.getRating(),
                        feedback.getRecommendation(),
                        feedback.getStrengths() != null ? feedback.getStrengths() : "",
                        feedback.getWeaknesses() != null ? feedback.getWeaknesses() : "",
                        feedback.getComments() != null ? feedback.getComments() : "",
                        feedback.getSubmittedAt()
                );
            }
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateQuestionsCsv(List<Question> questions) throws IOException {
        String[] headers = {"id", "title", "description", "category", "difficulty", "type",
                "expectedDurationMinutes", "tags", "isActive", "createdAt"};

        StringWriter writer = new StringWriter();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (Question question : questions) {
                printer.printRecord(
                        question.getId(),
                        question.getTitle(),
                        question.getDescription() != null ? question.getDescription() : "",
                        question.getCategory() != null ? question.getCategory().getId() : "",
                        question.getDifficulty(),
                        question.getType(),
                        question.getExpectedDurationMinutes() != null ? question.getExpectedDurationMinutes() : "",
                        question.getTags() != null ? question.getTags() : "",
                        question.getIsActive(),
                        question.getCreatedAt()
                );
            }
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== JSON Generators ====================

    private byte[] generateInterviewsJson(List<Interview> interviews) throws IOException {
        List<Map<String, Object>> data = interviews.stream().map(interview -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", interview.getId());
            map.put("title", interview.getTitle());
            map.put("status", interview.getStatus());
            map.put("type", interview.getType());
            map.put("mode", interview.getMode());
            map.put("candidateId", interview.getCandidate() != null ? interview.getCandidate().getId() : null);
            map.put("candidateName", interview.getCandidate() != null ?
                    interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName() : null);
            map.put("scheduledById", interview.getScheduledBy() != null ? interview.getScheduledBy().getId() : null);
            map.put("startTime", interview.getStartTime());
            map.put("endTime", interview.getEndTime());
            map.put("timeZone", interview.getTimeZone());
            map.put("meetingLink", interview.getMeetingLink());
            map.put("location", interview.getLocation());
            map.put("createdAt", interview.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return generateJson(data);
    }

    private byte[] generateCandidatesJson(Set<User> candidates) throws IOException {
        List<Map<String, Object>> data = candidates.stream().map(candidate -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", candidate.getId());
            map.put("firstName", candidate.getFirstName());
            map.put("lastName", candidate.getLastName());
            map.put("email", candidate.getEmail());
            map.put("phoneNumber", candidate.getPhoneNumber());
            map.put("status", candidate.getStatus());
            map.put("createdAt", candidate.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return generateJson(data);
    }

    private byte[] generateFeedbackJson(List<InterviewFeedBack> feedbacks) throws IOException {
        List<Map<String, Object>> data = feedbacks.stream().map(feedback -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", feedback.getId());
            map.put("interviewId", feedback.getInterview() != null ? feedback.getInterview().getId() : null);
            map.put("interviewerId", feedback.getInterviewer() != null ? feedback.getInterviewer().getId() : null);
            map.put("rating", feedback.getRating());
            map.put("recommendation", feedback.getRecommendation());
            map.put("strengths", feedback.getStrengths());
            map.put("weaknesses", feedback.getWeaknesses());
            map.put("comments", feedback.getComments());
            map.put("submittedAt", feedback.getSubmittedAt());
            return map;
        }).collect(Collectors.toList());

        return generateJson(data);
    }

    private byte[] generateQuestionsJson(List<Question> questions) throws IOException {
        List<Map<String, Object>> data = questions.stream().map(question -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", question.getId());
            map.put("title", question.getTitle());
            map.put("description", question.getDescription());
            map.put("categoryId", question.getCategory() != null ? question.getCategory().getId() : null);
            map.put("difficulty", question.getDifficulty());
            map.put("type", question.getType());
            map.put("expectedDurationMinutes", question.getExpectedDurationMinutes());
            map.put("tags", question.getTags());
            map.put("isActive", question.getIsActive());
            map.put("createdAt", question.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return generateJson(data);
    }

    // ==================== Private Helpers ====================

    private byte[] generateJson(List<Map<String, Object>> data) throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
    }

    private void uploadBytes(byte[] content, String s3Key, String contentType) {
        // Use S3StorageService's presigned upload or fall back to direct byte upload
        // The S3StorageService uses MultipartFile, so we use a direct approach here
        try {
            // We rely on the S3 client being accessible via presigned URL generation
            // For actual upload, we generate a presigned PUT URL and upload via HTTP,
            // or we directly call the S3 client. Since S3StorageService doesn't expose
            // a byte[] upload method, we use the presigned upload URL approach.
            // For simplicity in this implementation, we store the content and mark the key.
            // In production, this would use the S3 client directly.
            String presignedUrl = s3StorageService.generatePresignedUploadUrl(s3Key, contentType);
            // Upload using presigned URL
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(presignedUrl))
                    .header("Content-Type", contentType)
                    .PUT(java.net.http.HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
            httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload export file to S3: " + e.getMessage(), e);
        }
    }

    private String serializeFilters(Map<String, String> filters) {
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (IOException e) {
            return "{}";
        }
    }

    public Map<String, String> deserializeFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) return null;
        try {
            return objectMapper.readValue(filtersJson, Map.class);
        } catch (IOException e) {
            return null;
        }
    }
}

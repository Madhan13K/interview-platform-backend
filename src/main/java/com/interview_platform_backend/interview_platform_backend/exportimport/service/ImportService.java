package com.interview_platform_backend.interview_platform_backend.exportimport.service;

import com.interview_platform_backend.interview_platform_backend.document.entity.Document;
import com.interview_platform_backend.interview_platform_backend.document.repository.DocumentRepository;
import com.interview_platform_backend.interview_platform_backend.document.service.S3StorageService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobFormat;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobStatus;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobType;
import com.interview_platform_backend.interview_platform_backend.exportimport.repository.ExportImportJobRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final ExportImportJobRepository jobRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public ImportService(ExportImportJobRepository jobRepository,
                         DocumentRepository documentRepository,
                         UserRepository userRepository,
                         S3StorageService s3StorageService) {
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.s3StorageService = s3StorageService;
    }

    public ExportImportJob createImportJob(String entityType, UUID fileDocumentId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found: " + userId));

        Document document = documentRepository.findById(fileDocumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", fileDocumentId));

        // Determine format from file extension
        JobFormat format = determineFormat(document.getOriginalFileName());

        ExportImportJob job = ExportImportJob.builder()
                .organizationId(userId) // Using userId as org placeholder
                .user(user)
                .type(JobType.IMPORT)
                .format(format)
                .status(JobStatus.PENDING)
                .entityType(entityType)
                .fileName(document.getOriginalFileName())
                .s3Key(document.getS3Key())
                .totalRecords(0)
                .processedRecords(0)
                .build();

        return jobRepository.save(job);
    }

    @Async
    public void importCandidates(UUID jobId, UUID fileDocumentId) {
        ExportImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            Document document = documentRepository.findById(fileDocumentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", "id", fileDocumentId));

            // Download file from S3 using presigned URL
            String downloadUrl = s3StorageService.generatePresignedDownloadUrl(document.getS3Key());
            byte[] fileContent = downloadFile(downloadUrl);

            JobFormat format = determineFormat(document.getOriginalFileName());
            List<String> errors = new ArrayList<>();
            int totalRecords = 0;
            int processedRecords = 0;

            if (format == JobFormat.CSV) {
                // Parse CSV and create candidate records
                Reader reader = new InputStreamReader(
                        new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
                        .parse(reader);

                List<CSVRecord> records = parser.getRecords();
                totalRecords = records.size();
                job.setTotalRecords(totalRecords);
                jobRepository.save(job);

                for (int i = 0; i < records.size(); i++) {
                    CSVRecord record = records.get(i);
                    try {
                        String firstName = getFieldSafe(record, "firstName");
                        String lastName = getFieldSafe(record, "lastName");
                        String email = getFieldSafe(record, "email");

                        if (firstName == null || firstName.isBlank()) {
                            errors.add("Row " + (i + 1) + ": firstName is required");
                            continue;
                        }
                        if (lastName == null || lastName.isBlank()) {
                            errors.add("Row " + (i + 1) + ": lastName is required");
                            continue;
                        }
                        if (email == null || email.isBlank()) {
                            errors.add("Row " + (i + 1) + ": email is required");
                            continue;
                        }

                        // Check if user already exists
                        if (userRepository.findByEmail(email).isPresent()) {
                            errors.add("Row " + (i + 1) + ": user with email " + email + " already exists");
                            continue;
                        }

                        User candidate = User.builder()
                                .firstName(firstName)
                                .lastName(lastName)
                                .email(email)
                                .password("") // Imported candidates need password reset
                                .phoneNumber(getFieldSafe(record, "phoneNumber"))
                                .createdAt(Instant.now())
                                .build();

                        userRepository.save(candidate);
                        processedRecords++;

                        // Update progress periodically
                        if (processedRecords % 10 == 0) {
                            job.setProcessedRecords(processedRecords);
                            jobRepository.save(job);
                        }

                    } catch (Exception e) {
                        errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    }
                }
            } else {
                // TODO: Add Excel import support with Apache POI
                errors.add("Only CSV format is currently supported for import");
            }

            job.setProcessedRecords(processedRecords);
            job.setTotalRecords(totalRecords);

            if (errors.isEmpty()) {
                job.setStatus(JobStatus.COMPLETED);
            } else if (processedRecords > 0) {
                job.setStatus(JobStatus.COMPLETED);
                job.setErrorMessage("Completed with " + errors.size() + " errors: " + String.join("; ", errors));
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("All records failed: " + String.join("; ", errors));
            }

            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            log.info("Import job {} completed. {}/{} records processed. {} errors.",
                    jobId, processedRecords, totalRecords, errors.size());

        } catch (Exception e) {
            log.error("Import job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    private byte[] downloadFile(String presignedUrl) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to download file from S3. Status: " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    private JobFormat determineFormat(String fileName) {
        if (fileName == null) return JobFormat.CSV;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".json")) return JobFormat.JSON;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return JobFormat.EXCEL;
        return JobFormat.CSV;
    }

    private String getFieldSafe(CSVRecord record, String field) {
        try {
            return record.get(field);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.interview_platform_backend.interview_platform_backend.exportimport.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportImportJobResponse;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ImportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobFormat;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobStatus;
import com.interview_platform_backend.interview_platform_backend.exportimport.repository.ExportImportJobRepository;
import com.interview_platform_backend.interview_platform_backend.document.service.S3StorageService;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExportImportService {

    private final ExportImportJobRepository jobRepository;
    private final ExportService exportService;
    private final ImportService importService;
    private final S3StorageService s3StorageService;

    public ExportImportService(ExportImportJobRepository jobRepository,
                               ExportService exportService,
                               ImportService importService,
                               S3StorageService s3StorageService) {
        this.jobRepository = jobRepository;
        this.exportService = exportService;
        this.importService = importService;
        this.s3StorageService = s3StorageService;
    }

    public ExportImportJobResponse startExport(ExportRequest request, UUID userId) {
        String entityType = request.getEntityType().toUpperCase();
        JobFormat format = parseFormat(request.getFormat());
        Map<String, String> filters = request.getFilters();

        ExportImportJob job = exportService.createExportJob(entityType, format, filters, userId);

        // Delegate async processing based on entity type
        switch (entityType) {
            case "INTERVIEWS" -> exportService.exportInterviews(job.getId(), filters, format);
            case "CANDIDATES" -> exportService.exportCandidates(job.getId(), filters, format);
            case "FEEDBACK" -> exportService.exportFeedback(job.getId(), filters, format);
            case "QUESTIONS" -> exportService.exportQuestions(job.getId(), filters, format);
            default -> throw new BadRequestException("Unsupported entity type: " + entityType +
                    ". Supported: INTERVIEWS, CANDIDATES, FEEDBACK, QUESTIONS");
        }

        return mapToResponse(job);
    }

    public ExportImportJobResponse startImport(ImportRequest request, UUID userId) {
        String entityType = request.getEntityType().toUpperCase();

        if (!entityType.equals("CANDIDATES")) {
            throw new BadRequestException("Import is currently supported only for CANDIDATES entity type");
        }

        ExportImportJob job = importService.createImportJob(entityType, request.getFileDocumentId(), userId);

        // Delegate async processing
        importService.importCandidates(job.getId(), request.getFileDocumentId());

        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public ExportImportJobResponse getJob(UUID jobId) {
        ExportImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportImportJob", "id", jobId));
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ExportImportJobResponse> getMyJobs(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ExportImportJob> jobPage = jobRepository.findByUserId(userId, pageRequest);

        List<ExportImportJobResponse> content = jobPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.<ExportImportJobResponse>builder()
                .content(content)
                .page(jobPage.getNumber())
                .size(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .last(jobPage.isLast())
                .build();
    }

    public ExportImportJobResponse cancelJob(UUID jobId) {
        ExportImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportImportJob", "id", jobId));

        if (job.getStatus() != JobStatus.PENDING) {
            throw new BadRequestException("Only PENDING jobs can be cancelled. Current status: " + job.getStatus());
        }

        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage("Cancelled by user");
        job.setCompletedAt(java.time.Instant.now());
        jobRepository.save(job);

        return mapToResponse(job);
    }

    // ==================== Private Helpers ====================

    private ExportImportJobResponse mapToResponse(ExportImportJob job) {
        String downloadUrl = null;
        if (job.getStatus() == JobStatus.COMPLETED && job.getS3Key() != null) {
            try {
                downloadUrl = s3StorageService.generatePresignedDownloadUrl(job.getS3Key());
            } catch (Exception e) {
                // S3 may not be configured; leave download URL null
            }
        }

        Double progress = null;
        if (job.getTotalRecords() != null && job.getTotalRecords() > 0 && job.getProcessedRecords() != null) {
            progress = (double) job.getProcessedRecords() / job.getTotalRecords() * 100.0;
        } else if (job.getStatus() == JobStatus.COMPLETED) {
            progress = 100.0;
        } else if (job.getStatus() == JobStatus.PENDING) {
            progress = 0.0;
        }

        return ExportImportJobResponse.builder()
                .id(job.getId())
                .type(job.getType().name())
                .format(job.getFormat().name())
                .status(job.getStatus().name())
                .entityType(job.getEntityType())
                .fileName(job.getFileName())
                .totalRecords(job.getTotalRecords())
                .processedRecords(job.getProcessedRecords())
                .errorMessage(job.getErrorMessage())
                .downloadUrl(downloadUrl)
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .progress(progress)
                .build();
    }

    private JobFormat parseFormat(String format) {
        try {
            return JobFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid format: " + format + ". Supported: CSV, EXCEL, JSON");
        }
    }
}

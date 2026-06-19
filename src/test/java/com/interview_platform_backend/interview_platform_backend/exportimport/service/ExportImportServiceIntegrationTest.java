package com.interview_platform_backend.interview_platform_backend.exportimport.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.document.service.S3StorageService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportImportJobResponse;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ImportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob;
import com.interview_platform_backend.interview_platform_backend.exportimport.repository.ExportImportJobRepository;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class ExportImportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExportImportService exportImportService;

    @Autowired
    private ExportImportJobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private S3Presigner s3Presigner;

    @MockitoBean
    private S3StorageService s3StorageService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .firstName("Export")
                .lastName("Tester")
                .email("export-tester-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
    }

    @Nested
    @DisplayName("Start Export")
    class StartExport {

        @Test
        @DisplayName("should create export job with PENDING status for INTERVIEWS")
        void startExport_interviews_success() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("INTERVIEWS")
                    .format("CSV")
                    .filters(Map.of("status", "SCHEDULED"))
                    .build();

            ExportImportJobResponse response = exportImportService.startExport(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getType()).isEqualTo("EXPORT");
            assertThat(response.getFormat()).isEqualTo("CSV");
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getEntityType()).isEqualTo("INTERVIEWS");
            assertThat(response.getProgress()).isEqualTo(0.0);
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should create export job for CANDIDATES entity type")
        void startExport_candidates_success() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("CANDIDATES")
                    .format("JSON")
                    .build();

            ExportImportJobResponse response = exportImportService.startExport(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getEntityType()).isEqualTo("CANDIDATES");
            assertThat(response.getFormat()).isEqualTo("JSON");
            assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("should create export job for FEEDBACK entity type")
        void startExport_feedback_success() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("FEEDBACK")
                    .format("CSV")
                    .build();

            ExportImportJobResponse response = exportImportService.startExport(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getEntityType()).isEqualTo("FEEDBACK");
        }

        @Test
        @DisplayName("should create export job for QUESTIONS entity type")
        void startExport_questions_success() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("QUESTIONS")
                    .format("EXCEL")
                    .build();

            ExportImportJobResponse response = exportImportService.startExport(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getEntityType()).isEqualTo("QUESTIONS");
            assertThat(response.getFormat()).isEqualTo("EXCEL");
        }

        @Test
        @DisplayName("should throw BadRequestException for unsupported entity type")
        void startExport_unsupportedEntityType() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("UNKNOWN")
                    .format("CSV")
                    .build();

            assertThatThrownBy(() -> exportImportService.startExport(request, testUser.getId()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid format")
        void startExport_invalidFormat() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("INTERVIEWS")
                    .format("INVALID")
                    .build();

            assertThatThrownBy(() -> exportImportService.startExport(request, testUser.getId()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException when user not found")
        void startExport_userNotFound() {
            UUID nonExistentUserId = UUID.randomUUID();
            ExportRequest request = ExportRequest.builder()
                    .entityType("INTERVIEWS")
                    .format("CSV")
                    .build();

            assertThatThrownBy(() -> exportImportService.startExport(request, nonExistentUserId))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Start Import")
    class StartImport {

        @Test
        @DisplayName("should throw BadRequestException for non-CANDIDATES entity type")
        void startImport_unsupportedEntityType() {
            ImportRequest request = ImportRequest.builder()
                    .entityType("INTERVIEWS")
                    .fileDocumentId(UUID.randomUUID())
                    .build();

            assertThatThrownBy(() -> exportImportService.startImport(request, testUser.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("CANDIDATES");
        }
    }

    @Nested
    @DisplayName("Get Job")
    class GetJob {

        @Test
        @DisplayName("should return job details by id")
        void getJob_success() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("INTERVIEWS")
                    .format("CSV")
                    .build();

            ExportImportJobResponse created = exportImportService.startExport(request, testUser.getId());

            ExportImportJobResponse found = exportImportService.getJob(created.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getType()).isEqualTo("EXPORT");
            assertThat(found.getEntityType()).isEqualTo("INTERVIEWS");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when job not found")
        void getJob_notFound() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> exportImportService.getJob(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get My Jobs")
    class GetMyJobs {

        @Test
        @DisplayName("should return paginated results")
        void getMyJobs_paginated() {
            // Create multiple jobs
            exportImportService.startExport(ExportRequest.builder()
                    .entityType("INTERVIEWS").format("CSV").build(), testUser.getId());
            exportImportService.startExport(ExportRequest.builder()
                    .entityType("CANDIDATES").format("JSON").build(), testUser.getId());

            PaginatedResponse<ExportImportJobResponse> response =
                    exportImportService.getMyJobs(testUser.getId(), 0, 10);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty page when no jobs")
        void getMyJobs_empty() {
            PaginatedResponse<ExportImportJobResponse> response =
                    exportImportService.getMyJobs(testUser.getId(), 0, 10);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cancel Job")
    class CancelJob {

        @Test
        @DisplayName("should cancel a PENDING job successfully")
        void cancelJob_success() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("INTERVIEWS")
                    .format("CSV")
                    .build();

            ExportImportJobResponse created = exportImportService.startExport(request, testUser.getId());

            ExportImportJobResponse cancelled = exportImportService.cancelJob(created.getId());

            assertThat(cancelled.getStatus()).isEqualTo("FAILED");
            assertThat(cancelled.getErrorMessage()).isEqualTo("Cancelled by user");
            assertThat(cancelled.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when job not found")
        void cancelJob_notFound() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> exportImportService.cancelJob(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException when cancelling non-PENDING job")
        void cancelJob_notPending() {
            ExportRequest request = ExportRequest.builder()
                    .entityType("INTERVIEWS")
                    .format("CSV")
                    .build();

            ExportImportJobResponse created = exportImportService.startExport(request, testUser.getId());

            // Manually update job status to PROCESSING
            ExportImportJob job = jobRepository.findById(created.getId()).orElseThrow();
            job.setStatus(ExportImportJob.JobStatus.PROCESSING);
            jobRepository.save(job);

            assertThatThrownBy(() -> exportImportService.cancelJob(created.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING");
        }
    }
}

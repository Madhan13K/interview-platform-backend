package com.interview_platform_backend.interview_platform_backend.exportimport.controller;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportImportJobResponse;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ImportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.service.ExportImportService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportImportControllerWebMvcTest {

    private MockMvc mockMvc;
    private ExportImportService exportImportService;
    private SecurityHelper securityHelper;

    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        exportImportService = mock(ExportImportService.class);
        securityHelper = mock(SecurityHelper.class);
        ExportImportController controller = new ExportImportController(exportImportService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        currentUserId = UUID.randomUUID();
        given(securityHelper.getCurrentUserId()).willReturn(currentUserId);
    }

    private ExportImportJobResponse buildJobResponse(UUID id, String type, String entityType) {
        return ExportImportJobResponse.builder()
                .id(id)
                .type(type)
                .format("CSV")
                .status("PENDING")
                .entityType(entityType)
                .totalRecords(0)
                .processedRecords(0)
                .progress(0.0)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/export-import/export")
    class StartExportEndpoint {

        @Test
        @DisplayName("should start export and return 202")
        void startExport_returnsAccepted() throws Exception {
            UUID jobId = UUID.randomUUID();
            given(exportImportService.startExport(any(ExportRequest.class), eq(currentUserId)))
                    .willReturn(buildJobResponse(jobId, "EXPORT", "INTERVIEWS"));

            String body = """
                    {
                      "entityType": "INTERVIEWS",
                      "format": "CSV",
                      "filters": {"status": "SCHEDULED"}
                    }
                    """;

            mockMvc.perform(post("/api/v1/export-import/export")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(jobId.toString()))
                    .andExpect(jsonPath("$.type").value("EXPORT"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.entityType").value("INTERVIEWS"));

            verify(exportImportService).startExport(any(ExportRequest.class), eq(currentUserId));
        }

        @Test
        @DisplayName("should return 400 when entityType is blank")
        void startExport_blankEntityType_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "entityType": "",
                      "format": "CSV"
                    }
                    """;

            mockMvc.perform(post("/api/v1/export-import/export")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when format is blank")
        void startExport_blankFormat_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "entityType": "INTERVIEWS",
                      "format": ""
                    }
                    """;

            mockMvc.perform(post("/api/v1/export-import/export")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when service throws BadRequestException")
        void startExport_badRequest_returns400() throws Exception {
            given(exportImportService.startExport(any(ExportRequest.class), eq(currentUserId)))
                    .willThrow(new BadRequestException("Unsupported entity type: UNKNOWN"));

            String body = """
                    {
                      "entityType": "UNKNOWN",
                      "format": "CSV"
                    }
                    """;

            mockMvc.perform(post("/api/v1/export-import/export")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/export-import/import")
    class StartImportEndpoint {

        @Test
        @DisplayName("should start import and return 202")
        void startImport_returnsAccepted() throws Exception {
            UUID jobId = UUID.randomUUID();
            UUID fileDocumentId = UUID.randomUUID();
            given(exportImportService.startImport(any(ImportRequest.class), eq(currentUserId)))
                    .willReturn(buildJobResponse(jobId, "IMPORT", "CANDIDATES"));

            String body = """
                    {
                      "entityType": "CANDIDATES",
                      "fileDocumentId": "%s"
                    }
                    """.formatted(fileDocumentId);

            mockMvc.perform(post("/api/v1/export-import/import")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(jobId.toString()))
                    .andExpect(jsonPath("$.type").value("IMPORT"))
                    .andExpect(jsonPath("$.entityType").value("CANDIDATES"));

            verify(exportImportService).startImport(any(ImportRequest.class), eq(currentUserId));
        }

        @Test
        @DisplayName("should return 400 when entityType is blank")
        void startImport_blankEntityType_returnsBadRequest() throws Exception {
            UUID fileDocumentId = UUID.randomUUID();
            String body = """
                    {
                      "entityType": "",
                      "fileDocumentId": "%s"
                    }
                    """.formatted(fileDocumentId);

            mockMvc.perform(post("/api/v1/export-import/import")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when fileDocumentId is null")
        void startImport_nullFileDocumentId_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "entityType": "CANDIDATES"
                    }
                    """;

            mockMvc.perform(post("/api/v1/export-import/import")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for unsupported entity type")
        void startImport_unsupportedEntity_returnsBadRequest() throws Exception {
            UUID fileDocumentId = UUID.randomUUID();
            given(exportImportService.startImport(any(ImportRequest.class), eq(currentUserId)))
                    .willThrow(new BadRequestException("Import is currently supported only for CANDIDATES entity type"));

            String body = """
                    {
                      "entityType": "INTERVIEWS",
                      "fileDocumentId": "%s"
                    }
                    """.formatted(fileDocumentId);

            mockMvc.perform(post("/api/v1/export-import/import")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/export-import/jobs")
    class GetMyJobsEndpoint {

        @Test
        @DisplayName("should return paginated jobs")
        void getMyJobs_returnsOk() throws Exception {
            UUID jobId1 = UUID.randomUUID();
            UUID jobId2 = UUID.randomUUID();
            PaginatedResponse<ExportImportJobResponse> paginatedResponse = PaginatedResponse.<ExportImportJobResponse>builder()
                    .content(List.of(
                            buildJobResponse(jobId1, "EXPORT", "INTERVIEWS"),
                            buildJobResponse(jobId2, "EXPORT", "CANDIDATES")
                    ))
                    .page(0)
                    .size(10)
                    .totalElements(2L)
                    .totalPages(1)
                    .last(true)
                    .build();

            given(exportImportService.getMyJobs(currentUserId, 0, 10)).willReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/export-import/jobs")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.totalElements").value(2));

            verify(exportImportService).getMyJobs(currentUserId, 0, 10);
        }

        @Test
        @DisplayName("should use default pagination parameters")
        void getMyJobs_defaultParams_returnsOk() throws Exception {
            PaginatedResponse<ExportImportJobResponse> paginatedResponse = PaginatedResponse.<ExportImportJobResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(20)
                    .totalElements(0L)
                    .totalPages(0)
                    .last(true)
                    .build();

            given(exportImportService.getMyJobs(currentUserId, 0, 20)).willReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/export-import/jobs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0));

            verify(exportImportService).getMyJobs(currentUserId, 0, 20);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/export-import/jobs/{id}")
    class GetJobEndpoint {

        @Test
        @DisplayName("should return job details")
        void getJob_returnsOk() throws Exception {
            UUID jobId = UUID.randomUUID();
            ExportImportJobResponse response = buildJobResponse(jobId, "EXPORT", "INTERVIEWS");
            response.setStatus("COMPLETED");
            response.setProgress(100.0);
            response.setTotalRecords(50);
            response.setProcessedRecords(50);
            response.setFileName("interviews_export_123.csv");
            response.setDownloadUrl("https://s3.amazonaws.com/bucket/exports/file.csv");

            given(exportImportService.getJob(jobId)).willReturn(response);

            mockMvc.perform(get("/api/v1/export-import/jobs/{id}", jobId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(jobId.toString()))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.progress").value(100.0))
                    .andExpect(jsonPath("$.totalRecords").value(50))
                    .andExpect(jsonPath("$.downloadUrl").exists());

            verify(exportImportService).getJob(jobId);
        }

        @Test
        @DisplayName("should return 404 when job not found")
        void getJob_notFound_returns404() throws Exception {
            UUID jobId = UUID.randomUUID();
            given(exportImportService.getJob(jobId))
                    .willThrow(new ResourceNotFoundException("ExportImportJob", "id", jobId));

            mockMvc.perform(get("/api/v1/export-import/jobs/{id}", jobId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/export-import/jobs/{id}")
    class CancelJobEndpoint {

        @Test
        @DisplayName("should cancel job and return 200")
        void cancelJob_returnsOk() throws Exception {
            UUID jobId = UUID.randomUUID();
            ExportImportJobResponse response = buildJobResponse(jobId, "EXPORT", "INTERVIEWS");
            response.setStatus("FAILED");
            response.setErrorMessage("Cancelled by user");

            given(exportImportService.cancelJob(jobId)).willReturn(response);

            mockMvc.perform(delete("/api/v1/export-import/jobs/{id}", jobId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.errorMessage").value("Cancelled by user"));

            verify(exportImportService).cancelJob(jobId);
        }

        @Test
        @DisplayName("should return 404 when job not found")
        void cancelJob_notFound_returns404() throws Exception {
            UUID jobId = UUID.randomUUID();
            given(exportImportService.cancelJob(jobId))
                    .willThrow(new ResourceNotFoundException("ExportImportJob", "id", jobId));

            mockMvc.perform(delete("/api/v1/export-import/jobs/{id}", jobId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when job cannot be cancelled")
        void cancelJob_notPending_returnsBadRequest() throws Exception {
            UUID jobId = UUID.randomUUID();
            given(exportImportService.cancelJob(jobId))
                    .willThrow(new BadRequestException("Only PENDING jobs can be cancelled. Current status: PROCESSING"));

            mockMvc.perform(delete("/api/v1/export-import/jobs/{id}", jobId))
                    .andExpect(status().isBadRequest());
        }
    }
}

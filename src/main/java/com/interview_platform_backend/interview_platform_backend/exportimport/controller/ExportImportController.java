package com.interview_platform_backend.interview_platform_backend.exportimport.controller;

import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportImportJobResponse;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ExportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.dto.ImportRequest;
import com.interview_platform_backend.interview_platform_backend.exportimport.service.ExportImportService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export-import")
@Tag(name = "Export/Import", description = "Export and import interview platform data (interviews, candidates, feedback, questions)")
public class ExportImportController {

    private final ExportImportService exportImportService;
    private final SecurityHelper securityHelper;

    public ExportImportController(ExportImportService exportImportService, SecurityHelper securityHelper) {
        this.exportImportService = exportImportService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Start an export job", description = "Initiates an async export job for the specified entity type and format")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Export job accepted and processing started"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @PostMapping("/export")
    public ResponseEntity<ExportImportJobResponse> startExport(@RequestBody @Valid ExportRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        ExportImportJobResponse response = exportImportService.startExport(request, currentUserId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Start an import job", description = "Initiates an async import job from an uploaded file")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Import job accepted and processing started"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "File document not found")
    })
    @PostMapping("/import")
    public ResponseEntity<ExportImportJobResponse> startImport(@RequestBody @Valid ImportRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        ExportImportJobResponse response = exportImportService.startImport(request, currentUserId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Get my export/import jobs", description = "Returns paginated list of the current user's export/import jobs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully")
    })
    @GetMapping("/jobs")
    public ResponseEntity<PaginatedResponse<ExportImportJobResponse>> getMyJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        PaginatedResponse<ExportImportJobResponse> response = exportImportService.getMyJobs(currentUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get job status/details", description = "Returns details of a specific export/import job including download URL if completed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job details retrieved"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    @GetMapping("/jobs/{id}")
    public ResponseEntity<ExportImportJobResponse> getJob(@PathVariable UUID id) {
        ExportImportJobResponse response = exportImportService.getJob(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel a pending job", description = "Cancels a job only if it is still in PENDING status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Job cannot be cancelled (not in PENDING status)"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<ExportImportJobResponse> cancelJob(@PathVariable UUID id) {
        ExportImportJobResponse response = exportImportService.cancelJob(id);
        return ResponseEntity.ok(response);
    }
}

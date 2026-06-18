package com.interview_platform_backend.interview_platform_backend.bulk.controller;

import com.interview_platform_backend.interview_platform_backend.bulk.dto.*;
import com.interview_platform_backend.interview_platform_backend.bulk.service.BulkOperationService;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bulk")
@Tag(name = "Bulk Operations", description = "Bulk schedule interviews, invite candidates, and export data")
public class BulkOperationController {

    private final BulkOperationService bulkOperationService;
    private final SecurityHelper securityHelper;

    public BulkOperationController(BulkOperationService bulkOperationService, SecurityHelper securityHelper) {
        this.bulkOperationService = bulkOperationService;
        this.securityHelper = securityHelper;
    }

    // ==================== Bulk Schedule ====================

    @Operation(summary = "Bulk schedule multiple interviews at once")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk scheduling completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/interviews/schedule")
    @PreAuthorize("hasAuthority('INTERVIEW_CREATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<BulkOperationResponse<InterviewResponse>> bulkScheduleInterviews(
            @RequestBody @Valid BulkScheduleInterviewsRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(bulkOperationService.bulkScheduleInterviews(request, currentUserId));
    }

    // ==================== Bulk Invite ====================

    @Operation(summary = "Bulk invite candidates to an interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk invite completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/candidates/invite")
    @PreAuthorize("hasAuthority('INTERVIEW_CREATE') or hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<BulkOperationResponse<BulkInviteResult>> bulkInviteCandidates(
            @RequestBody @Valid BulkInviteCandidatesRequest request) {
        return ResponseEntity.ok(bulkOperationService.bulkInviteCandidates(request));
    }

    // ==================== Bulk Export ====================

    @Operation(summary = "Bulk export interview data as CSV or JSON")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Export generated"),
            @ApiResponse(responseCode = "400", description = "Invalid export request")
    })
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<byte[]> bulkExportData(
            @RequestParam BulkExportRequest.ExportType exportType,
            @RequestParam(defaultValue = "CSV") BulkExportRequest.ExportFormat format,
            @RequestParam(required = false) InterviewStatus statusFilter,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {

        BulkExportRequest request = BulkExportRequest.builder()
                .exportType(exportType)
                .format(format)
                .statusFilter(statusFilter)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        byte[] data = bulkOperationService.bulkExportData(request);

        String contentType;
        String fileExtension;
        if (format == BulkExportRequest.ExportFormat.JSON) {
            contentType = MediaType.APPLICATION_JSON_VALUE;
            fileExtension = "json";
        } else {
            contentType = "text/csv";
            fileExtension = "csv";
        }

        String fileName = exportType.name().toLowerCase() + "_export." + fileExtension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }
}


package com.interview_platform_backend.interview_platform_backend.jobboard.controller;

import com.interview_platform_backend.interview_platform_backend.jobboard.dto.JobListingResponse;
import com.interview_platform_backend.interview_platform_backend.jobboard.service.JobBoardService;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.EmploymentType;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.ExperienceLevel;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Public Job Board", description = "Public job listings - no authentication required")
public class PublicJobBoardController {

    private final JobBoardService jobBoardService;

    public PublicJobBoardController(JobBoardService jobBoardService) {
        this.jobBoardService = jobBoardService;
    }

    @Operation(summary = "Get paginated public job listings (only OPEN positions)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job listings retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<JobListingResponse>> getPublicJobListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobBoardService.getPublicJobListings(page, size));
    }

    @Operation(summary = "Get a single job listing detail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job listing found"),
            @ApiResponse(responseCode = "404", description = "Job listing not found or not open")
    })
    @GetMapping("/{id}")
    public ResponseEntity<JobListingResponse> getJobListingDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(jobBoardService.getJobListingDetail(id));
    }

    @Operation(summary = "Search public job listings with filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results")
    })
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<JobListingResponse>> searchPublicJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EmploymentType type,
            @RequestParam(required = false) ExperienceLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobBoardService.searchPublicJobs(keyword, department, location, type, level, page, size));
    }
}

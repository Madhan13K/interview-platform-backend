package com.interview_platform_backend.interview_platform_backend.ai.controller;

import com.interview_platform_backend.interview_platform_backend.ai.dto.*;
import com.interview_platform_backend.interview_platform_backend.ai.service.AiService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "AI-Powered Features API")
@PreAuthorize("isAuthenticated()")
public class AiController {

    private final AiService aiService;
    private final SecurityHelper securityHelper;

    public AiController(AiService aiService, SecurityHelper securityHelper) {
        this.aiService = aiService;
        this.securityHelper = securityHelper;
    }

    @PostMapping("/suggest-questions")
    @Operation(summary = "Generate AI question suggestions", description = "Generates interview question suggestions based on job title, difficulty, and category")
    @ApiResponse(responseCode = "201", description = "Questions generated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<AiResponse> suggestQuestions(@Valid @RequestBody AiQuestionSuggestionRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        AiResponse response = aiService.suggestQuestions(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/parse-resume")
    @Operation(summary = "Parse resume using AI", description = "Parses an uploaded resume document and extracts structured data")
    @ApiResponse(responseCode = "201", description = "Resume parsed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<AiResponse> parseResume(@Valid @RequestBody AiResumeParsRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        AiResponse response = aiService.parseResume(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/interview-summary")
    @Operation(summary = "Generate interview summary", description = "Generates an AI-powered summary from interview feedback")
    @ApiResponse(responseCode = "201", description = "Interview summary generated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<AiResponse> generateInterviewSummary(@Valid @RequestBody AiInterviewSummaryRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        AiResponse response = aiService.generateInterviewSummary(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get AI suggestion history", description = "Returns paginated list of AI suggestions for the current user")
    @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully")
    public ResponseEntity<PaginatedResponse<AiResponse>> getSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = securityHelper.getCurrentUserId();
        PaginatedResponse<AiResponse> response = aiService.getSuggestions(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions/interview/{interviewId}")
    @Operation(summary = "Get suggestions for an interview", description = "Returns all AI suggestions associated with a specific interview")
    @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully")
    public ResponseEntity<List<AiResponse>> getSuggestionsByInterview(@PathVariable UUID interviewId) {
        List<AiResponse> response = aiService.getSuggestionsByInterview(interviewId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/suggestions/{id}/status")
    @Operation(summary = "Update suggestion status", description = "Accept or reject an AI suggestion")
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status value")
    @ApiResponse(responseCode = "404", description = "Suggestion not found")
    public ResponseEntity<AiResponse> updateSuggestionStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        AiResponse response = aiService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }
}

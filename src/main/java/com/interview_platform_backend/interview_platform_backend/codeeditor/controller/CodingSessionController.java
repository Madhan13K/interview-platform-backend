package com.interview_platform_backend.interview_platform_backend.codeeditor.controller;

import com.interview_platform_backend.interview_platform_backend.codeeditor.dto.CodingSessionResponse;
import com.interview_platform_backend.interview_platform_backend.codeeditor.service.CodingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/code")
@Tag(name = "Code Editor", description = "Collaborative code editor session management")
public class CodingSessionController {

    private final CodingSessionService codingSessionService;

    public CodingSessionController(CodingSessionService codingSessionService) {
        this.codingSessionService = codingSessionService;
    }

    @Operation(summary = "Start or join a coding session for an interview")
    @ApiResponse(responseCode = "200", description = "Session started or existing session returned")
    @PostMapping("/start")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<CodingSessionResponse> startSession(
            @PathVariable UUID interviewId,
            @RequestParam(defaultValue = "java") String language) {
        return ResponseEntity.ok(codingSessionService.startSession(interviewId, language));
    }

    @Operation(summary = "Get active coding session for an interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active session found"),
            @ApiResponse(responseCode = "404", description = "No active session")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<CodingSessionResponse> getActiveSession(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(codingSessionService.getActiveSession(interviewId));
    }

    @Operation(summary = "Save code snapshot (auto-save from editor)")
    @ApiResponse(responseCode = "200", description = "Code saved")
    @PutMapping("/save")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<CodingSessionResponse> saveCode(
            @PathVariable UUID interviewId,
            @RequestBody SaveCodeRequest request) {
        return ResponseEntity.ok(codingSessionService.saveCode(
                interviewId, request.code(), request.language(), request.userId()));
    }

    @Operation(summary = "End the coding session")
    @ApiResponse(responseCode = "204", description = "Session ended")
    @PostMapping("/end")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER')")
    public ResponseEntity<Void> endSession(@PathVariable UUID interviewId) {
        codingSessionService.endSession(interviewId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get coding session history for an interview")
    @ApiResponse(responseCode = "200", description = "List of past sessions")
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<List<CodingSessionResponse>> getSessionHistory(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(codingSessionService.getSessionHistory(interviewId));
    }

    public record SaveCodeRequest(String code, String language, UUID userId) {}
}


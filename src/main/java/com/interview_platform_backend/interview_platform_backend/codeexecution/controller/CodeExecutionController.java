package com.interview_platform_backend.interview_platform_backend.codeexecution.controller;

import com.interview_platform_backend.interview_platform_backend.codeexecution.dto.CodeExecutionRequest;
import com.interview_platform_backend.interview_platform_backend.codeexecution.dto.CodeExecutionResponse;
import com.interview_platform_backend.interview_platform_backend.codeexecution.service.CodeExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/code-execution")
@Tag(name = "Code Execution", description = "Sandboxed code execution engine for coding assessments")
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;

    public CodeExecutionController(CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    @Operation(summary = "Execute code in a sandboxed Docker container",
            description = "Submits code for execution. Returns immediately with a queued status. " +
                    "Poll the GET endpoint for results.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execution submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request (unsupported language, code too large, etc.)"),
            @ApiResponse(responseCode = "429", description = "Too many concurrent executions")
    })
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<CodeExecutionResponse> executeCode(
            @RequestBody @Valid CodeExecutionRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(codeExecutionService.submitExecution(request, userEmail));
    }

    @Operation(summary = "Get execution result by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execution result found"),
            @ApiResponse(responseCode = "404", description = "Execution not found")
    })
    @GetMapping("/{executionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<CodeExecutionResponse> getExecution(@PathVariable UUID executionId) {
        return ResponseEntity.ok(codeExecutionService.getExecution(executionId));
    }

    @Operation(summary = "Get all executions for a coding session")
    @ApiResponse(responseCode = "200", description = "List of executions")
    @GetMapping("/session/{codingSessionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<List<CodeExecutionResponse>> getExecutionsForSession(
            @PathVariable UUID codingSessionId) {
        return ResponseEntity.ok(codeExecutionService.getExecutionsForSession(codingSessionId));
    }

    @Operation(summary = "Get list of supported programming languages")
    @ApiResponse(responseCode = "200", description = "Supported languages list")
    @GetMapping("/languages")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<List<String>> getSupportedLanguages() {
        return ResponseEntity.ok(codeExecutionService.getSupportedLanguages());
    }
}

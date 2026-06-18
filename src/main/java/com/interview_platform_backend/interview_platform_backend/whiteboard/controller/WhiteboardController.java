package com.interview_platform_backend.interview_platform_backend.whiteboard.controller;

import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.*;
import com.interview_platform_backend.interview_platform_backend.whiteboard.service.WhiteboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whiteboards")
@Tag(name = "Whiteboard", description = "Whiteboard collaboration session management")
public class WhiteboardController {

    private final WhiteboardService whiteboardService;
    private final SecurityHelper securityHelper;

    public WhiteboardController(WhiteboardService whiteboardService, SecurityHelper securityHelper) {
        this.whiteboardService = whiteboardService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Create a new whiteboard session")
    @ApiResponse(responseCode = "201", description = "Whiteboard session created")
    @PostMapping
    public ResponseEntity<WhiteboardSessionResponse> createSession(@Valid @RequestBody CreateWhiteboardRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        WhiteboardSessionResponse response = whiteboardService.createSession(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a whiteboard session by ID")
    @ApiResponse(responseCode = "200", description = "Whiteboard session found")
    @ApiResponse(responseCode = "404", description = "Whiteboard session not found")
    @GetMapping("/{id}")
    public ResponseEntity<WhiteboardSessionResponse> getSession(@PathVariable UUID id) {
        return ResponseEntity.ok(whiteboardService.getSession(id));
    }

    @Operation(summary = "Get all whiteboard sessions for an interview")
    @ApiResponse(responseCode = "200", description = "List of whiteboard sessions")
    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<List<WhiteboardSessionResponse>> getSessionsByInterview(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(whiteboardService.getSessionsByInterview(interviewId));
    }

    @Operation(summary = "Add a stroke to a whiteboard session")
    @ApiResponse(responseCode = "201", description = "Stroke added")
    @ApiResponse(responseCode = "400", description = "Session is closed or invalid tool")
    @PostMapping("/{id}/strokes")
    public ResponseEntity<WhiteboardStrokeResponse> addStroke(
            @PathVariable UUID id,
            @Valid @RequestBody AddStrokeRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        WhiteboardStrokeResponse response = whiteboardService.addStroke(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all strokes for a whiteboard session")
    @ApiResponse(responseCode = "200", description = "List of strokes")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @GetMapping("/{id}/strokes")
    public ResponseEntity<List<WhiteboardStrokeResponse>> getStrokes(@PathVariable UUID id) {
        return ResponseEntity.ok(whiteboardService.getStrokes(id));
    }

    @Operation(summary = "Save a snapshot of the whiteboard state")
    @ApiResponse(responseCode = "200", description = "Snapshot saved")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @PutMapping("/{id}/snapshot")
    public ResponseEntity<WhiteboardSessionResponse> saveSnapshot(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String snapshotData = body.get("snapshotData");
        return ResponseEntity.ok(whiteboardService.saveSnapshot(id, snapshotData));
    }

    @Operation(summary = "Close a whiteboard session")
    @ApiResponse(responseCode = "200", description = "Session closed")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @PatchMapping("/{id}/close")
    public ResponseEntity<WhiteboardSessionResponse> closeSession(@PathVariable UUID id) {
        return ResponseEntity.ok(whiteboardService.closeSession(id));
    }

    @Operation(summary = "Delete a whiteboard session")
    @ApiResponse(responseCode = "204", description = "Session deleted")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        whiteboardService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}

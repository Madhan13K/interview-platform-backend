package com.interview_platform_backend.interview_platform_backend.video.controller;

import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.video.dto.StartRecordingRequest;
import com.interview_platform_backend.interview_platform_backend.video.dto.VideoRecordingResponse;
import com.interview_platform_backend.interview_platform_backend.video.service.VideoRecordingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/video-recordings")
@Tag(name = "Video Recordings", description = "Video recording management for interviews")
public class VideoRecordingController {

    private final VideoRecordingService videoRecordingService;
    private final SecurityHelper securityHelper;

    public VideoRecordingController(VideoRecordingService videoRecordingService,
                                    SecurityHelper securityHelper) {
        this.videoRecordingService = videoRecordingService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Start a new video recording",
            description = "Initiates a video recording for the specified interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recording started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @PostMapping("/start")
    public ResponseEntity<VideoRecordingResponse> startRecording(
            @RequestBody @Valid StartRecordingRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(videoRecordingService.startRecording(request, userId));
    }

    @Operation(summary = "Mark a recording as complete",
            description = "Updates the recording status to READY with file size and duration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recording marked as complete"),
            @ApiResponse(responseCode = "400", description = "Recording is not in PROCESSING status"),
            @ApiResponse(responseCode = "404", description = "Recording not found")
    })
    @PatchMapping("/{id}/complete")
    public ResponseEntity<VideoRecordingResponse> completeRecording(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        Long fileSizeBytes = body.get("fileSizeBytes") != null
                ? ((Number) body.get("fileSizeBytes")).longValue() : null;
        Integer durationSeconds = body.get("durationSeconds") != null
                ? ((Number) body.get("durationSeconds")).intValue() : null;
        return ResponseEntity.ok(videoRecordingService.completeRecording(id, fileSizeBytes, durationSeconds));
    }

    @Operation(summary = "Mark a recording as failed",
            description = "Updates the recording status to FAILED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recording marked as failed"),
            @ApiResponse(responseCode = "400", description = "Recording is not in PROCESSING status"),
            @ApiResponse(responseCode = "404", description = "Recording not found")
    })
    @PatchMapping("/{id}/fail")
    public ResponseEntity<VideoRecordingResponse> failRecording(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(videoRecordingService.failRecording(id, reason));
    }

    @Operation(summary = "Get all recordings for an interview",
            description = "Returns a list of video recordings associated with the specified interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recordings retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<List<VideoRecordingResponse>> getRecordingsByInterview(
            @PathVariable UUID interviewId) {
        return ResponseEntity.ok(videoRecordingService.getRecordingsByInterview(interviewId));
    }

    @Operation(summary = "Get a single recording",
            description = "Returns a video recording with a presigned download URL if ready")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recording retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Recording not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<VideoRecordingResponse> getRecording(@PathVariable UUID id) {
        return ResponseEntity.ok(videoRecordingService.getRecording(id));
    }

    @Operation(summary = "Delete a recording",
            description = "Soft deletes a recording by marking its status as DELETED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recording deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Recording not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<VideoRecordingResponse> deleteRecording(@PathVariable UUID id) {
        return ResponseEntity.ok(videoRecordingService.deleteRecording(id));
    }

    @Operation(summary = "Get my recordings",
            description = "Returns a paginated list of recordings made by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recordings retrieved successfully")
    })
    @GetMapping("/my")
    public ResponseEntity<PaginatedResponse<VideoRecordingResponse>> getMyRecordings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(videoRecordingService.getMyRecordings(userId, page, size));
    }
}

package com.interview_platform_backend.interview_platform_backend.meeting.controller;

import com.interview_platform_backend.interview_platform_backend.meeting.dto.GenerateMeetingRequest;
import com.interview_platform_backend.interview_platform_backend.meeting.dto.MeetingLinkResponse;
import com.interview_platform_backend.interview_platform_backend.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/meeting")
@Tag(name = "Meeting", description = "Video meeting link generation and management")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @Operation(summary = "Generate a meeting link for an interview",
            description = "Supports ZOOM, GOOGLE_MEET, TEAMS, or INTERNAL providers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meeting link generated"),
            @ApiResponse(responseCode = "404", description = "Interview not found"),
            @ApiResponse(responseCode = "409", description = "Meeting link already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<MeetingLinkResponse> generateMeetingLink(
            @PathVariable UUID interviewId,
            @RequestBody @Valid GenerateMeetingRequest request) {
        return ResponseEntity.ok(meetingService.generateMeetingLink(interviewId, request));
    }

    @Operation(summary = "Get meeting link for an interview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meeting link found"),
            @ApiResponse(responseCode = "404", description = "No meeting link for this interview")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('INTERVIEWER') or hasRole('CANDIDATE')")
    public ResponseEntity<MeetingLinkResponse> getMeetingLink(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(meetingService.getMeetingLink(interviewId));
    }
}


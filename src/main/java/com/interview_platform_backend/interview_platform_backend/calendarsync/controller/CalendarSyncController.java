package com.interview_platform_backend.interview_platform_backend.calendarsync.controller;

import com.interview_platform_backend.interview_platform_backend.calendarsync.dto.*;
import com.interview_platform_backend.interview_platform_backend.calendarsync.service.CalendarSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar-sync")
@Tag(name = "Calendar Sync", description = "Bidirectional Google Calendar and Outlook sync endpoints")
public class CalendarSyncController {

    private final CalendarSyncService calendarSyncService;

    public CalendarSyncController(CalendarSyncService calendarSyncService) {
        this.calendarSyncService = calendarSyncService;
    }

    @Operation(summary = "Connect an external calendar")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calendar connected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or connection already exists")
    })
    @PostMapping("/connect")
    public ResponseEntity<CalendarConnectionResponse> connectCalendar(
            @RequestBody @Valid CalendarConnectionRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(calendarSyncService.connectCalendar(request, userEmail));
    }

    @Operation(summary = "Disconnect an external calendar")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Calendar disconnected successfully"),
            @ApiResponse(responseCode = "404", description = "Connection not found")
    })
    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> disconnectCalendar(
            @PathVariable UUID id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        calendarSyncService.disconnectCalendar(id, userEmail);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List all calendar connections for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of calendar connections")
    @GetMapping("/connections")
    public ResponseEntity<List<CalendarConnectionResponse>> getConnections(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(calendarSyncService.getConnections(userEmail));
    }

    @Operation(summary = "Sync a single interview to all connected calendars")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "404", description = "Interview not found"),
            @ApiResponse(responseCode = "400", description = "No active calendar connections")
    })
    @PostMapping("/sync/interview/{interviewId}")
    public ResponseEntity<SyncResultResponse> syncInterview(
            @PathVariable UUID interviewId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(calendarSyncService.syncInterviewToCalendar(interviewId, userEmail));
    }

    @Operation(summary = "Sync all upcoming interviews to connected calendars")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk sync completed"),
            @ApiResponse(responseCode = "400", description = "No active calendar connections")
    })
    @PostMapping("/sync/all")
    public ResponseEntity<SyncResultResponse> syncAllInterviews(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(calendarSyncService.syncAllInterviews(userEmail));
    }

    @Operation(summary = "Trigger full bidirectional sync for a specific connection")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bidirectional sync completed"),
            @ApiResponse(responseCode = "404", description = "Connection not found"),
            @ApiResponse(responseCode = "400", description = "Invalid connection ownership")
    })
    @PostMapping("/sync/bidirectional/{connectionId}")
    public ResponseEntity<SyncResultResponse> triggerBidirectionalSync(
            @PathVariable UUID connectionId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(calendarSyncService.triggerBidirectionalSync(connectionId, userEmail));
    }

    @Operation(summary = "Get all synced calendar events for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of synced events")
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getSyncedEvents(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(calendarSyncService.getSyncedEvents(userEmail));
    }
}

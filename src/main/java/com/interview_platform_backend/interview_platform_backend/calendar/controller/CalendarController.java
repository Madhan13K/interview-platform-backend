package com.interview_platform_backend.interview_platform_backend.calendar.controller;

import com.interview_platform_backend.interview_platform_backend.calendar.dto.AvailabilityResponse;
import com.interview_platform_backend.interview_platform_backend.calendar.dto.AvailabilitySlot;
import com.interview_platform_backend.interview_platform_backend.calendar.dto.CreateAvailabilityRequest;
import com.interview_platform_backend.interview_platform_backend.calendar.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
@Tag(name = "Calendar", description = "Interviewer availability and scheduling")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Operation(summary = "Add availability slot for an interviewer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability added"),
            @ApiResponse(responseCode = "400", description = "Invalid time range"),
            @ApiResponse(responseCode = "404", description = "Interviewer not found")
    })
    @PostMapping("/interviewers/{interviewerId}/availability")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<AvailabilityResponse> addAvailability(
            @PathVariable UUID interviewerId,
            @RequestBody @Valid CreateAvailabilityRequest request) {
        return ResponseEntity.ok(calendarService.addAvailability(interviewerId, request));
    }

    @Operation(summary = "Get all availability slots for an interviewer")
    @ApiResponse(responseCode = "200", description = "List of availability slots")
    @GetMapping("/interviewers/{interviewerId}/availability")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(
            @PathVariable UUID interviewerId) {
        return ResponseEntity.ok(calendarService.getAvailability(interviewerId));
    }

    @Operation(summary = "Check interviewer availability for a specific date",
            description = "Returns time slots with availability status considering existing interviews")
    @ApiResponse(responseCode = "200", description = "Available slots for the date")
    @GetMapping("/interviewers/{interviewerId}/availability/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<List<AvailabilitySlot>> checkAvailability(
            @PathVariable UUID interviewerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(calendarService.getAvailabilityForDate(interviewerId, date));
    }

    @Operation(summary = "Delete an availability slot")
    @ApiResponse(responseCode = "204", description = "Availability removed")
    @DeleteMapping("/interviewers/{interviewerId}/availability/{availabilityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER')")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable UUID interviewerId,
            @PathVariable UUID availabilityId) {
        calendarService.deleteAvailability(interviewerId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}


package com.interview_platform_backend.interview_platform_backend.scheduling.controller;

import com.interview_platform_backend.interview_platform_backend.scheduling.dto.*;
import com.interview_platform_backend.interview_platform_backend.scheduling.service.SchedulingService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduling")
@Tag(name = "Automated Scheduling", description = "Manage availability and auto-suggest interview time slots")
public class SchedulingController {

    private final SchedulingService schedulingService;
    private final SecurityHelper securityHelper;

    public SchedulingController(SchedulingService schedulingService, SecurityHelper securityHelper) {
        this.schedulingService = schedulingService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Add an availability slot for current user")
    @PostMapping("/availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvailabilitySlotResponse> addAvailability(
            @RequestBody @Valid CreateAvailabilitySlotRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(schedulingService.createSlot(request, userId));
    }

    @Operation(summary = "Get my availability slots")
    @GetMapping("/availability/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AvailabilitySlotResponse>> getMyAvailability() {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(schedulingService.getUserAvailability(userId));
    }

    @Operation(summary = "Get availability for a specific user")
    @GetMapping("/availability/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> getUserAvailability(@PathVariable UUID userId) {
        return ResponseEntity.ok(schedulingService.getUserAvailability(userId));
    }

    @Operation(summary = "Delete an availability slot")
    @DeleteMapping("/availability/{slotId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSlot(@PathVariable UUID slotId) {
        UUID userId = securityHelper.getCurrentUserId();
        schedulingService.deleteSlot(slotId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Auto-suggest time slots based on interviewer availability")
    @PostMapping("/suggest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<SuggestedTimeSlot>> suggestTimeSlots(
            @RequestBody @Valid SuggestTimeSlotsRequest request) {
        return ResponseEntity.ok(schedulingService.suggestTimeSlots(request));
    }
}


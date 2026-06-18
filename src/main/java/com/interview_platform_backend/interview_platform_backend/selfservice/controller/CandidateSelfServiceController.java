package com.interview_platform_backend.interview_platform_backend.selfservice.controller;

import com.interview_platform_backend.interview_platform_backend.selfservice.dto.*;
import com.interview_platform_backend.interview_platform_backend.selfservice.entity.CandidatePreferredSlot;
import com.interview_platform_backend.interview_platform_backend.selfservice.service.CandidateSelfServiceImpl;
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
@RequestMapping("/api/v1/self-service")
@Tag(name = "Candidate Self-Service", description = "Candidates pick preferred time slots, submit availability")
public class CandidateSelfServiceController {

    private final CandidateSelfServiceImpl selfService;
    private final SecurityHelper securityHelper;

    public CandidateSelfServiceController(CandidateSelfServiceImpl selfService, SecurityHelper securityHelper) {
        this.selfService = selfService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Submit preferred time slots")
    @PostMapping("/preferred-slots")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<PreferredSlotResponse> submitPreferredSlot(
            @RequestBody @Valid SubmitPreferredSlotRequest request) {
        UUID candidateId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(selfService.submitPreferredSlot(request, candidateId));
    }

    @Operation(summary = "Get my preferred slots")
    @GetMapping("/preferred-slots/my")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<List<PreferredSlotResponse>> getMySlots() {
        UUID candidateId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(selfService.getMyPreferredSlots(candidateId));
    }

    @Operation(summary = "Get preferred slots for an interview (recruiter view)")
    @GetMapping("/preferred-slots/interview/{interviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<PreferredSlotResponse>> getSlotsByInterview(@PathVariable UUID interviewId) {
        return ResponseEntity.ok(selfService.getSlotsByInterview(interviewId));
    }

    @Operation(summary = "Get preferred slots for a job position")
    @GetMapping("/preferred-slots/job-position/{jobPositionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<PreferredSlotResponse>> getSlotsByJobPosition(@PathVariable UUID jobPositionId) {
        return ResponseEntity.ok(selfService.getSlotsByJobPosition(jobPositionId));
    }

    @Operation(summary = "Accept/reject a candidate's preferred slot")
    @PatchMapping("/preferred-slots/{slotId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<PreferredSlotResponse> updateStatus(
            @PathVariable UUID slotId,
            @RequestParam CandidatePreferredSlot.SlotStatus status) {
        return ResponseEntity.ok(selfService.updateSlotStatus(slotId, status));
    }

    @Operation(summary = "Delete a preferred slot")
    @DeleteMapping("/preferred-slots/{slotId}")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSlot(@PathVariable UUID slotId) {
        UUID candidateId = securityHelper.getCurrentUserId();
        selfService.deleteSlot(slotId, candidateId);
        return ResponseEntity.noContent().build();
    }
}


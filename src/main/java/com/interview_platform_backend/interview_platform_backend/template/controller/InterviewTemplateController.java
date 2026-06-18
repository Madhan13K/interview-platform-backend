package com.interview_platform_backend.interview_platform_backend.template.controller;

import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.template.dto.*;
import com.interview_platform_backend.interview_platform_backend.template.service.InterviewTemplateService;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class InterviewTemplateController {

    private final InterviewTemplateService templateService;
    private final SecurityHelper securityHelper;

    public InterviewTemplateController(InterviewTemplateService templateService,
                                        SecurityHelper securityHelper) {
        this.templateService = templateService;
        this.securityHelper = securityHelper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        TemplateResponse response = templateService.createTemplate(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable UUID templateId) {
        return ResponseEntity.ok(templateService.getTemplate(templateId));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/paginated")
    public ResponseEntity<PaginatedResponse<TemplateResponse>> getTemplatesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(templateService.getTemplatesPaginated(page, size));
    }

    @GetMapping("/filter/type")
    public ResponseEntity<List<TemplateResponse>> getTemplatesByType(@RequestParam InterviewType type) {
        return ResponseEntity.ok(templateService.getTemplatesByType(type));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TemplateResponse>> searchTemplates(@RequestParam String keyword) {
        return ResponseEntity.ok(templateService.searchTemplates(keyword));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<TemplateResponse> addQuestionToTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody TemplateQuestionRequest request) {
        return ResponseEntity.ok(templateService.addQuestionToTemplate(templateId, request));
    }

    @DeleteMapping("/{templateId}/questions/{templateQuestionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<TemplateResponse> removeQuestionFromTemplate(
            @PathVariable UUID templateId,
            @PathVariable UUID templateQuestionId) {
        return ResponseEntity.ok(templateService.removeQuestionFromTemplate(templateId, templateQuestionId));
    }

    @PostMapping("/create-interview")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'INTERVIEWER')")
    public ResponseEntity<InterviewResponse> createInterviewFromTemplate(
            @Valid @RequestBody CreateInterviewFromTemplateRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        InterviewResponse response = templateService.createInterviewFromTemplate(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}


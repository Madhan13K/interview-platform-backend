package com.interview_platform_backend.interview_platform_backend.questionbank.controller;

import com.interview_platform_backend.interview_platform_backend.questionbank.dto.*;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionDifficulty;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionType;
import com.interview_platform_backend.interview_platform_backend.questionbank.service.QuestionBankService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions")
@Tag(name = "Question Bank", description = "Categorized interview questions management")
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final SecurityHelper securityHelper;

    public QuestionBankController(QuestionBankService questionBankService, SecurityHelper securityHelper) {
        this.questionBankService = questionBankService;
        this.securityHelper = securityHelper;
    }

    // ==================== Categories ====================

    @Operation(summary = "Create a question category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category created"),
            @ApiResponse(responseCode = "409", description = "Category name already exists")
    })
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER')")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        return ResponseEntity.ok(questionBankService.createCategory(request));
    }

    @Operation(summary = "Get all question categories")
    @ApiResponse(responseCode = "200", description = "List of categories")
    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(questionBankService.getAllCategories());
    }

    // ==================== Questions ====================

    @Operation(summary = "Create a new question")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question created"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER')")
    public ResponseEntity<QuestionResponse> createQuestion(@RequestBody @Valid CreateQuestionRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(questionBankService.createQuestion(request, currentUserId));
    }

    @Operation(summary = "Get a question by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question found"),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @GetMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable UUID questionId) {
        return ResponseEntity.ok(questionBankService.getQuestion(questionId));
    }

    @Operation(summary = "Update a question")
    @ApiResponse(responseCode = "200", description = "Question updated")
    @PutMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER')")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable UUID questionId,
            @RequestBody @Valid CreateQuestionRequest request) {
        return ResponseEntity.ok(questionBankService.updateQuestion(questionId, request));
    }

    @Operation(summary = "Delete a question (soft delete)")
    @ApiResponse(responseCode = "204", description = "Question deactivated")
    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID questionId) {
        questionBankService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search questions with filters",
            description = "Filter by category, difficulty, type, keyword. Supports pagination.")
    @ApiResponse(responseCode = "200", description = "Paginated question results")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<PaginatedResponse<QuestionResponse>> searchQuestions(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) QuestionDifficulty difficulty,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(questionBankService.searchQuestions(
                categoryId, difficulty, type, keyword, page, size));
    }

    @Operation(summary = "Get questions by category")
    @ApiResponse(responseCode = "200", description = "List of questions in category")
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERVIEWER') or hasRole('RECRUITER')")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(questionBankService.getQuestionsByCategory(categoryId));
    }
}


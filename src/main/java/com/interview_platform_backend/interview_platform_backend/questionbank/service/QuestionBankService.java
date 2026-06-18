package com.interview_platform_backend.interview_platform_backend.questionbank.service;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.questionbank.dto.*;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.Question;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionCategory;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionDifficulty;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionType;
import com.interview_platform_backend.interview_platform_backend.questionbank.repository.QuestionCategoryRepository;
import com.interview_platform_backend.interview_platform_backend.questionbank.repository.QuestionRepository;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QuestionBankService {

    private final QuestionRepository questionRepository;
    private final QuestionCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public QuestionBankService(QuestionRepository questionRepository,
                               QuestionCategoryRepository categoryRepository,
                               UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    // ==================== Categories ====================

    @CacheEvict(value = "questionCategories", allEntries = true)
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        QuestionCategory category = QuestionCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        QuestionCategory saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    // ==================== Questions ====================

    @CacheEvict(value = "questions", allEntries = true)
    public QuestionResponse createQuestion(CreateQuestionRequest request, UUID createdByUserId) {
        QuestionCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        User createdBy = null;
        if (createdByUserId != null) {
            createdBy = userRepository.findById(createdByUserId).orElse(null);
        }

        Question question = Question.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .difficulty(request.getDifficulty())
                .type(request.getType())
                .expectedDurationMinutes(request.getExpectedDurationMinutes())
                .sampleAnswer(request.getSampleAnswer())
                .hints(request.getHints())
                .tags(request.getTags())
                .isActive(true)
                .createdBy(createdBy)
                .build();

        Question saved = questionRepository.save(question);
        return toQuestionResponse(saved);
    }

    @Cacheable(value = "questions", key = "#questionId")
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));
        return toQuestionResponse(question);
    }

    @CacheEvict(value = "questions", allEntries = true)
    public QuestionResponse updateQuestion(UUID questionId, CreateQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (request.getTitle() != null) question.setTitle(request.getTitle());
        if (request.getDescription() != null) question.setDescription(request.getDescription());
        if (request.getDifficulty() != null) question.setDifficulty(request.getDifficulty());
        if (request.getType() != null) question.setType(request.getType());
        if (request.getExpectedDurationMinutes() != null) question.setExpectedDurationMinutes(request.getExpectedDurationMinutes());
        if (request.getSampleAnswer() != null) question.setSampleAnswer(request.getSampleAnswer());
        if (request.getHints() != null) question.setHints(request.getHints());
        if (request.getTags() != null) question.setTags(request.getTags());

        if (request.getCategoryId() != null) {
            QuestionCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            question.setCategory(category);
        }

        Question saved = questionRepository.save(question);
        return toQuestionResponse(saved);
    }

    @CacheEvict(value = "questions", allEntries = true)
    public void deleteQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));
        // Soft delete
        question.setIsActive(false);
        questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<QuestionResponse> searchQuestions(
            UUID categoryId, QuestionDifficulty difficulty, QuestionType type,
            String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Question> questionPage = questionRepository.searchQuestions(
                categoryId, difficulty, type, keyword, pageable);

        List<QuestionResponse> content = questionPage.getContent().stream()
                .map(this::toQuestionResponse)
                .toList();

        return PaginatedResponse.<QuestionResponse>builder()
                .content(content)
                .page(questionPage.getNumber())
                .size(questionPage.getSize())
                .totalElements(questionPage.getTotalElements())
                .totalPages(questionPage.getTotalPages())
                .last(questionPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByCategory(UUID categoryId) {
        return questionRepository.findByCategoryIdAndIsActiveTrue(categoryId).stream()
                .map(this::toQuestionResponse)
                .toList();
    }

    // ==================== Mappers ====================

    private CategoryResponse toCategoryResponse(QuestionCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }

    private QuestionResponse toQuestionResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .description(question.getDescription())
                .categoryId(question.getCategory().getId())
                .categoryName(question.getCategory().getName())
                .difficulty(question.getDifficulty())
                .type(question.getType())
                .expectedDurationMinutes(question.getExpectedDurationMinutes())
                .sampleAnswer(question.getSampleAnswer())
                .hints(question.getHints())
                .tags(question.getTags())
                .isActive(question.getIsActive())
                .createdBy(question.getCreatedBy() != null ? question.getCreatedBy().getId() : null)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}


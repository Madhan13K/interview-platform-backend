package com.interview_platform_backend.interview_platform_backend.ai.service;

import com.interview_platform_backend.interview_platform_backend.ai.dto.*;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion.AiSuggestionStatus;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion.AiSuggestionType;
import com.interview_platform_backend.interview_platform_backend.ai.repository.AiSuggestionRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AiService {

    private final AiSuggestionRepository aiSuggestionRepository;
    private final UserRepository userRepository;

    public AiService(AiSuggestionRepository aiSuggestionRepository, UserRepository userRepository) {
        this.aiSuggestionRepository = aiSuggestionRepository;
        this.userRepository = userRepository;
    }

    @CircuitBreaker(name = "aiService", fallbackMethod = "suggestQuestionsFallback")
    public AiResponse suggestQuestions(AiQuestionSuggestionRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String prompt = buildQuestionPrompt(request);
        String aiOutput = callAiModel(prompt, AiSuggestionType.QUESTION_SUGGESTION);

        AiSuggestion suggestion = AiSuggestion.builder()
                .organizationId(userId)
                .user(user)
                .type(AiSuggestionType.QUESTION_SUGGESTION)
                .inputContext(prompt)
                .outputContent(aiOutput)
                .model("mock-gpt-4")
                .tokensUsed(150)
                .confidenceScore(0.85)
                .status(AiSuggestionStatus.GENERATED)
                .build();

        AiSuggestion saved = aiSuggestionRepository.save(suggestion);
        return mapToResponse(saved);
    }

    @CircuitBreaker(name = "aiService", fallbackMethod = "parseResumeFallback")
    public AiResponse parseResume(AiResumeParsRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String prompt = "Parse resume document with ID: " + request.getDocumentId();
        String aiOutput = callAiModel(prompt, AiSuggestionType.RESUME_PARSE);

        AiSuggestion suggestion = AiSuggestion.builder()
                .organizationId(userId)
                .user(user)
                .type(AiSuggestionType.RESUME_PARSE)
                .inputContext(prompt)
                .outputContent(aiOutput)
                .model("mock-gpt-4")
                .tokensUsed(200)
                .confidenceScore(0.90)
                .status(AiSuggestionStatus.GENERATED)
                .build();

        AiSuggestion saved = aiSuggestionRepository.save(suggestion);
        return mapToResponse(saved);
    }

    @CircuitBreaker(name = "aiService", fallbackMethod = "generateInterviewSummaryFallback")
    public AiResponse generateInterviewSummary(AiInterviewSummaryRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String prompt = "Generate interview summary for interview ID: " + request.getInterviewId();
        String aiOutput = callAiModel(prompt, AiSuggestionType.INTERVIEW_SUMMARY);

        AiSuggestion suggestion = AiSuggestion.builder()
                .organizationId(userId)
                .user(user)
                .type(AiSuggestionType.INTERVIEW_SUMMARY)
                .inputContext(prompt)
                .outputContent(aiOutput)
                .model("mock-gpt-4")
                .tokensUsed(300)
                .confidenceScore(0.80)
                .status(AiSuggestionStatus.GENERATED)
                .build();

        AiSuggestion saved = aiSuggestionRepository.save(suggestion);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<AiResponse> getSuggestions(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AiSuggestion> suggestionPage = aiSuggestionRepository.findByUserId(userId, pageRequest);

        List<AiResponse> content = suggestionPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.<AiResponse>builder()
                .content(content)
                .page(suggestionPage.getNumber())
                .size(suggestionPage.getSize())
                .totalElements(suggestionPage.getTotalElements())
                .totalPages(suggestionPage.getTotalPages())
                .last(suggestionPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AiResponse> getSuggestionsByInterview(UUID interviewId) {
        List<AiSuggestion> suggestions = aiSuggestionRepository.findByInterviewId(interviewId);
        return suggestions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AiResponse updateStatus(UUID id, String status) {
        AiSuggestion suggestion = aiSuggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AiSuggestion", "id", id));

        try {
            AiSuggestionStatus newStatus = AiSuggestionStatus.valueOf(status.toUpperCase());
            suggestion.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status + ". Must be ACCEPTED or REJECTED");
        }

        AiSuggestion saved = aiSuggestionRepository.save(suggestion);
        return mapToResponse(saved);
    }

    private String buildQuestionPrompt(AiQuestionSuggestionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate ").append(request.getCount())
                .append(" interview questions for the role: ").append(request.getJobTitle())
                .append(". Difficulty: ").append(request.getDifficulty())
                .append(". Category: ").append(request.getCategory());

        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            prompt.append(". Skills: ").append(String.join(", ", request.getSkills()));
        }

        return prompt.toString();
    }

    // TODO: Replace with actual AI provider (OpenAI, etc.)
    private String callAiModel(String prompt, AiSuggestionType type) {
        return switch (type) {
            case QUESTION_SUGGESTION -> """
                    [
                      {"question": "Explain the difference between abstract classes and interfaces in Java.", "difficulty": "MEDIUM", "category": "TECHNICAL"},
                      {"question": "Design a URL shortening service like bit.ly.", "difficulty": "HARD", "category": "SYSTEM_DESIGN"},
                      {"question": "Describe a time when you had to resolve a conflict within your team.", "difficulty": "MEDIUM", "category": "BEHAVIORAL"},
                      {"question": "Implement a function to detect a cycle in a linked list.", "difficulty": "MEDIUM", "category": "CODING"},
                      {"question": "What are the SOLID principles and why are they important?", "difficulty": "EASY", "category": "TECHNICAL"}
                    ]""";
            case RESUME_PARSE -> """
                    {
                      "candidateName": "John Doe",
                      "email": "john.doe@example.com",
                      "phone": "+1-555-0123",
                      "skills": ["Java", "Spring Boot", "Python", "AWS", "Docker", "Kubernetes"],
                      "experience": [
                        {"company": "Tech Corp", "role": "Senior Software Engineer", "duration": "2020-2023", "description": "Led backend development team"},
                        {"company": "StartupXYZ", "role": "Software Engineer", "duration": "2017-2020", "description": "Full-stack development"}
                      ],
                      "education": [
                        {"institution": "MIT", "degree": "M.S. Computer Science", "year": "2017"},
                        {"institution": "State University", "degree": "B.S. Computer Science", "year": "2015"}
                      ],
                      "summary": "Experienced software engineer with 6+ years in backend development, cloud services, and team leadership."
                    }""";
            case INTERVIEW_SUMMARY -> """
                    {
                      "overallRating": 4.2,
                      "strengths": ["Strong technical knowledge", "Good communication skills", "Problem-solving ability"],
                      "weaknesses": ["Could improve system design explanations", "Limited experience with distributed systems"],
                      "recommendation": "STRONG_HIRE",
                      "summary": "The candidate demonstrated solid technical skills and excellent problem-solving abilities. They communicated clearly and showed strong potential for growth in the role."
                    }""";
            case CANDIDATE_ASSESSMENT -> """
                    {
                      "technicalScore": 8.5,
                      "communicationScore": 7.5,
                      "problemSolvingScore": 9.0,
                      "cultureFitScore": 8.0,
                      "overallAssessment": "Strong candidate with excellent technical fundamentals."
                    }""";
        };
    }

    private AiResponse mapToResponse(AiSuggestion suggestion) {
        return AiResponse.builder()
                .id(suggestion.getId())
                .type(suggestion.getType())
                .outputContent(suggestion.getOutputContent())
                .model(suggestion.getModel())
                .tokensUsed(suggestion.getTokensUsed())
                .confidenceScore(suggestion.getConfidenceScore())
                .status(suggestion.getStatus())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }

    private String callAiModelFallback(String prompt, AiSuggestionType type, Throwable throwable) {
        return "{\"error\": \"AI service temporarily unavailable\", \"message\": \"" + throwable.getMessage() + "\"}";
    }

    private AiResponse suggestQuestionsFallback(AiQuestionSuggestionRequest request, UUID userId, Throwable throwable) {
        if (throwable instanceof ResourceNotFoundException || throwable instanceof BadRequestException) {
            throw (RuntimeException) throwable;
        }
        return AiResponse.builder()
                .type(AiSuggestionType.QUESTION_SUGGESTION)
                .outputContent("{\"error\": \"AI service temporarily unavailable\"}")
                .model("fallback")
                .status(AiSuggestionStatus.GENERATED)
                .build();
    }

    private AiResponse parseResumeFallback(AiResumeParsRequest request, UUID userId, Throwable throwable) {
        if (throwable instanceof ResourceNotFoundException || throwable instanceof BadRequestException) {
            throw (RuntimeException) throwable;
        }
        return AiResponse.builder()
                .type(AiSuggestionType.RESUME_PARSE)
                .outputContent("{\"error\": \"AI service temporarily unavailable\"}")
                .model("fallback")
                .status(AiSuggestionStatus.GENERATED)
                .build();
    }

    private AiResponse generateInterviewSummaryFallback(AiInterviewSummaryRequest request, UUID userId, Throwable throwable) {
        if (throwable instanceof ResourceNotFoundException || throwable instanceof BadRequestException) {
            throw (RuntimeException) throwable;
        }
        return AiResponse.builder()
                .type(AiSuggestionType.INTERVIEW_SUMMARY)
                .outputContent("{\"error\": \"AI service temporarily unavailable\"}")
                .model("fallback")
                .status(AiSuggestionStatus.GENERATED)
                .build();
    }
}

package com.interview_platform_backend.interview_platform_backend.ai.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.ai.dto.*;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion.AiSuggestionStatus;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion.AiSuggestionType;
import com.interview_platform_backend.interview_platform_backend.ai.repository.AiSuggestionRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class AiServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiSuggestionRepository aiSuggestionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "ai-test-" + UUID.randomUUID() + "@example.com";
        testUser = User.builder()
                .firstName("AI")
                .lastName("Tester")
                .email(testEmail)
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("Suggest Questions")
    class SuggestQuestions {

        @Test
        @DisplayName("should generate question suggestions successfully")
        void suggestQuestions_success() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Senior Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .skills(List.of("Java", "Spring Boot", "Microservices"))
                    .count(5)
                    .build();

            AiResponse response = aiService.suggestQuestions(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getType()).isEqualTo(AiSuggestionType.QUESTION_SUGGESTION);
            assertThat(response.getOutputContent()).isNotBlank();
            assertThat(response.getModel()).isEqualTo("mock-gpt-4");
            assertThat(response.getTokensUsed()).isEqualTo(150);
            assertThat(response.getConfidenceScore()).isEqualTo(0.85);
            assertThat(response.getStatus()).isEqualTo(AiSuggestionStatus.GENERATED);
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should generate questions with skills in output")
        void suggestQuestions_withSkills() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Python Developer")
                    .difficulty("HARD")
                    .category("CODING")
                    .skills(List.of("Python", "Django", "FastAPI"))
                    .count(3)
                    .build();

            AiResponse response = aiService.suggestQuestions(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getOutputContent()).contains("question");
        }

        @Test
        @DisplayName("should generate questions without skills")
        void suggestQuestions_withoutSkills() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Product Manager")
                    .difficulty("EASY")
                    .category("BEHAVIORAL")
                    .count(2)
                    .build();

            AiResponse response = aiService.suggestQuestions(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(AiSuggestionType.QUESTION_SUGGESTION);
        }

        @Test
        @DisplayName("should persist suggestion in database")
        void suggestQuestions_persistsInDatabase() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("DevOps Engineer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .skills(List.of("Docker", "Kubernetes"))
                    .count(5)
                    .build();

            AiResponse response = aiService.suggestQuestions(request, testUser.getId());

            AiSuggestion persisted = aiSuggestionRepository.findById(response.getId()).orElse(null);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getType()).isEqualTo(AiSuggestionType.QUESTION_SUGGESTION);
            assertThat(persisted.getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void suggestQuestions_userNotFound() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();

            UUID nonExistentUserId = UUID.randomUUID();
            assertThatThrownBy(() -> aiService.suggestQuestions(request, nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("Parse Resume")
    class ParseResume {

        @Test
        @DisplayName("should parse resume successfully")
        void parseResume_success() {
            AiResumeParsRequest request = AiResumeParsRequest.builder()
                    .documentId(UUID.randomUUID())
                    .build();

            AiResponse response = aiService.parseResume(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getType()).isEqualTo(AiSuggestionType.RESUME_PARSE);
            assertThat(response.getOutputContent()).contains("candidateName");
            assertThat(response.getModel()).isEqualTo("mock-gpt-4");
            assertThat(response.getTokensUsed()).isEqualTo(200);
            assertThat(response.getConfidenceScore()).isEqualTo(0.90);
            assertThat(response.getStatus()).isEqualTo(AiSuggestionStatus.GENERATED);
        }

        @Test
        @DisplayName("should persist parsed resume in database")
        void parseResume_persistsInDatabase() {
            AiResumeParsRequest request = AiResumeParsRequest.builder()
                    .documentId(UUID.randomUUID())
                    .build();

            AiResponse response = aiService.parseResume(request, testUser.getId());

            AiSuggestion persisted = aiSuggestionRepository.findById(response.getId()).orElse(null);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getType()).isEqualTo(AiSuggestionType.RESUME_PARSE);
            assertThat(persisted.getInputContext()).contains("Parse resume document");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void parseResume_userNotFound() {
            AiResumeParsRequest request = AiResumeParsRequest.builder()
                    .documentId(UUID.randomUUID())
                    .build();

            UUID nonExistentUserId = UUID.randomUUID();
            assertThatThrownBy(() -> aiService.parseResume(request, nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("Generate Interview Summary")
    class GenerateInterviewSummary {

        @Test
        @DisplayName("should generate interview summary successfully")
        void generateInterviewSummary_success() {
            AiInterviewSummaryRequest request = AiInterviewSummaryRequest.builder()
                    .interviewId(UUID.randomUUID())
                    .build();

            AiResponse response = aiService.generateInterviewSummary(request, testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getType()).isEqualTo(AiSuggestionType.INTERVIEW_SUMMARY);
            assertThat(response.getOutputContent()).contains("overallRating");
            assertThat(response.getOutputContent()).contains("recommendation");
            assertThat(response.getModel()).isEqualTo("mock-gpt-4");
            assertThat(response.getTokensUsed()).isEqualTo(300);
            assertThat(response.getConfidenceScore()).isEqualTo(0.80);
            assertThat(response.getStatus()).isEqualTo(AiSuggestionStatus.GENERATED);
        }

        @Test
        @DisplayName("should persist interview summary in database")
        void generateInterviewSummary_persistsInDatabase() {
            UUID interviewId = UUID.randomUUID();
            AiInterviewSummaryRequest request = AiInterviewSummaryRequest.builder()
                    .interviewId(interviewId)
                    .build();

            AiResponse response = aiService.generateInterviewSummary(request, testUser.getId());

            AiSuggestion persisted = aiSuggestionRepository.findById(response.getId()).orElse(null);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getType()).isEqualTo(AiSuggestionType.INTERVIEW_SUMMARY);
            assertThat(persisted.getInputContext()).contains(interviewId.toString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void generateInterviewSummary_userNotFound() {
            AiInterviewSummaryRequest request = AiInterviewSummaryRequest.builder()
                    .interviewId(UUID.randomUUID())
                    .build();

            UUID nonExistentUserId = UUID.randomUUID();
            assertThatThrownBy(() -> aiService.generateInterviewSummary(request, nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("Get Suggestions")
    class GetSuggestions {

        @Test
        @DisplayName("should return paginated suggestions")
        void getSuggestions_paginated() {
            // Create some suggestions first
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();

            aiService.suggestQuestions(request, testUser.getId());
            aiService.suggestQuestions(request, testUser.getId());
            aiService.suggestQuestions(request, testUser.getId());

            PaginatedResponse<AiResponse> response = aiService.getSuggestions(testUser.getId(), 0, 10);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(3);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isEqualTo(3L);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.getLast()).isTrue();
        }

        @Test
        @DisplayName("should return paginated suggestions with correct page size")
        void getSuggestions_withPageSize() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();

            for (int i = 0; i < 5; i++) {
                aiService.suggestQuestions(request, testUser.getId());
            }

            PaginatedResponse<AiResponse> page1 = aiService.getSuggestions(testUser.getId(), 0, 2);

            assertThat(page1.getContent()).hasSize(2);
            assertThat(page1.getTotalElements()).isEqualTo(5L);
            assertThat(page1.getTotalPages()).isEqualTo(3);
            assertThat(page1.getLast()).isFalse();
        }

        @Test
        @DisplayName("should return empty results for user with no suggestions")
        void getSuggestions_emptyResults() {
            PaginatedResponse<AiResponse> response = aiService.getSuggestions(testUser.getId(), 0, 10);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0L);
            assertThat(response.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("should not return suggestions from other users")
        void getSuggestions_isolatedByUser() {
            // Create suggestion for test user
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();
            aiService.suggestQuestions(request, testUser.getId());

            // Create another user
            String otherEmail = "other-ai-" + UUID.randomUUID() + "@example.com";
            User otherUser = User.builder()
                    .firstName("Other")
                    .lastName("User")
                    .email(otherEmail)
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            otherUser = userRepository.save(otherUser);

            PaginatedResponse<AiResponse> otherResponse = aiService.getSuggestions(otherUser.getId(), 0, 10);

            assertThat(otherResponse.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Suggestions By Interview")
    class GetSuggestionsByInterview {

        @Test
        @DisplayName("should return empty list for interview with no suggestions")
        void getSuggestionsByInterview_empty() {
            UUID interviewId = UUID.randomUUID();
            List<AiResponse> responses = aiService.getSuggestionsByInterview(interviewId);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Status")
    class UpdateStatus {

        @Test
        @DisplayName("should accept a suggestion")
        void updateStatus_accept() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();

            AiResponse created = aiService.suggestQuestions(request, testUser.getId());
            assertThat(created.getStatus()).isEqualTo(AiSuggestionStatus.GENERATED);

            AiResponse updated = aiService.updateStatus(created.getId(), "ACCEPTED");

            assertThat(updated.getStatus()).isEqualTo(AiSuggestionStatus.ACCEPTED);
            assertThat(updated.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("should reject a suggestion")
        void updateStatus_reject() {
            AiResumeParsRequest request = AiResumeParsRequest.builder()
                    .documentId(UUID.randomUUID())
                    .build();

            AiResponse created = aiService.parseResume(request, testUser.getId());

            AiResponse updated = aiService.updateStatus(created.getId(), "REJECTED");

            assertThat(updated.getStatus()).isEqualTo(AiSuggestionStatus.REJECTED);
        }

        @Test
        @DisplayName("should handle case-insensitive status")
        void updateStatus_caseInsensitive() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();

            AiResponse created = aiService.suggestQuestions(request, testUser.getId());
            AiResponse updated = aiService.updateStatus(created.getId(), "accepted");

            assertThat(updated.getStatus()).isEqualTo(AiSuggestionStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent suggestion")
        void updateStatus_notFound() {
            UUID randomId = UUID.randomUUID();
            assertThatThrownBy(() -> aiService.updateStatus(randomId, "ACCEPTED"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("AiSuggestion");
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid status")
        void updateStatus_invalidStatus() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();

            AiResponse created = aiService.suggestQuestions(request, testUser.getId());

            assertThatThrownBy(() -> aiService.updateStatus(created.getId(), "INVALID_STATUS"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid status");
        }

        @Test
        @DisplayName("should persist status change in database")
        void updateStatus_persistsInDatabase() {
            AiQuestionSuggestionRequest request = AiQuestionSuggestionRequest.builder()
                    .jobTitle("Java Developer")
                    .difficulty("MEDIUM")
                    .category("TECHNICAL")
                    .count(5)
                    .build();

            AiResponse created = aiService.suggestQuestions(request, testUser.getId());
            aiService.updateStatus(created.getId(), "ACCEPTED");

            AiSuggestion persisted = aiSuggestionRepository.findById(created.getId()).orElse(null);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getStatus()).isEqualTo(AiSuggestionStatus.ACCEPTED);
        }
    }
}

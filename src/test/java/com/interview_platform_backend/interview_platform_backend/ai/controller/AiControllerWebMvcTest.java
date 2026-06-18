package com.interview_platform_backend.interview_platform_backend.ai.controller;

import com.interview_platform_backend.interview_platform_backend.ai.dto.AiResponse;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion.AiSuggestionStatus;
import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion.AiSuggestionType;
import com.interview_platform_backend.interview_platform_backend.ai.service.AiService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiControllerWebMvcTest {

    private MockMvc mockMvc;
    private AiService aiService;
    private SecurityHelper securityHelper;

    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        aiService = mock(AiService.class);
        securityHelper = mock(SecurityHelper.class);
        currentUserId = UUID.randomUUID();
        given(securityHelper.getCurrentUserId()).willReturn(currentUserId);

        AiController controller = new AiController(aiService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AiResponse buildAiResponse(AiSuggestionType type) {
        return AiResponse.builder()
                .id(UUID.randomUUID())
                .type(type)
                .outputContent("Generated AI content")
                .model("mock-gpt-4")
                .tokensUsed(150)
                .confidenceScore(0.85)
                .status(AiSuggestionStatus.GENERATED)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/ai/suggest-questions")
    class SuggestQuestions {

        @Test
        @DisplayName("should generate questions and return 201")
        void suggestQuestions_success() throws Exception {
            AiResponse response = buildAiResponse(AiSuggestionType.QUESTION_SUGGESTION);
            given(aiService.suggestQuestions(any(), eq(currentUserId))).willReturn(response);

            String body = """
                    {
                      "jobTitle": "Senior Java Developer",
                      "difficulty": "MEDIUM",
                      "category": "TECHNICAL",
                      "skills": ["Java", "Spring Boot"],
                      "count": 5
                    }
                    """;

            mockMvc.perform(post("/api/v1/ai/suggest-questions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.getId().toString()))
                    .andExpect(jsonPath("$.type").value("QUESTION_SUGGESTION"))
                    .andExpect(jsonPath("$.outputContent").value("Generated AI content"))
                    .andExpect(jsonPath("$.model").value("mock-gpt-4"))
                    .andExpect(jsonPath("$.status").value("GENERATED"));

            verify(aiService).suggestQuestions(any(), eq(currentUserId));
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void suggestQuestions_missingFields() throws Exception {
            String body = """
                    {
                      "skills": ["Java"]
                    }
                    """;

            mockMvc.perform(post("/api/v1/ai/suggest-questions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void suggestQuestions_userNotFound() throws Exception {
            given(aiService.suggestQuestions(any(), eq(currentUserId)))
                    .willThrow(new ResourceNotFoundException("User", "id", currentUserId));

            String body = """
                    {
                      "jobTitle": "Java Developer",
                      "difficulty": "MEDIUM",
                      "category": "TECHNICAL",
                      "count": 5
                    }
                    """;

            mockMvc.perform(post("/api/v1/ai/suggest-questions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai/parse-resume")
    class ParseResume {

        @Test
        @DisplayName("should parse resume and return 201")
        void parseResume_success() throws Exception {
            AiResponse response = buildAiResponse(AiSuggestionType.RESUME_PARSE);
            given(aiService.parseResume(any(), eq(currentUserId))).willReturn(response);

            UUID documentId = UUID.randomUUID();
            String body = """
                    {
                      "documentId": "%s"
                    }
                    """.formatted(documentId);

            mockMvc.perform(post("/api/v1/ai/parse-resume")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.getId().toString()))
                    .andExpect(jsonPath("$.type").value("RESUME_PARSE"))
                    .andExpect(jsonPath("$.status").value("GENERATED"));

            verify(aiService).parseResume(any(), eq(currentUserId));
        }

        @Test
        @DisplayName("should return 400 for missing documentId")
        void parseResume_missingDocumentId() throws Exception {
            String body = """
                    {}
                    """;

            mockMvc.perform(post("/api/v1/ai/parse-resume")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void parseResume_userNotFound() throws Exception {
            given(aiService.parseResume(any(), eq(currentUserId)))
                    .willThrow(new ResourceNotFoundException("User", "id", currentUserId));

            UUID documentId = UUID.randomUUID();
            String body = """
                    {
                      "documentId": "%s"
                    }
                    """.formatted(documentId);

            mockMvc.perform(post("/api/v1/ai/parse-resume")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai/interview-summary")
    class GenerateInterviewSummary {

        @Test
        @DisplayName("should generate interview summary and return 201")
        void generateInterviewSummary_success() throws Exception {
            AiResponse response = buildAiResponse(AiSuggestionType.INTERVIEW_SUMMARY);
            given(aiService.generateInterviewSummary(any(), eq(currentUserId))).willReturn(response);

            UUID interviewId = UUID.randomUUID();
            String body = """
                    {
                      "interviewId": "%s"
                    }
                    """.formatted(interviewId);

            mockMvc.perform(post("/api/v1/ai/interview-summary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.getId().toString()))
                    .andExpect(jsonPath("$.type").value("INTERVIEW_SUMMARY"))
                    .andExpect(jsonPath("$.model").value("mock-gpt-4"));

            verify(aiService).generateInterviewSummary(any(), eq(currentUserId));
        }

        @Test
        @DisplayName("should return 400 for missing interviewId")
        void generateInterviewSummary_missingInterviewId() throws Exception {
            String body = """
                    {}
                    """;

            mockMvc.perform(post("/api/v1/ai/interview-summary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void generateInterviewSummary_userNotFound() throws Exception {
            given(aiService.generateInterviewSummary(any(), eq(currentUserId)))
                    .willThrow(new ResourceNotFoundException("User", "id", currentUserId));

            UUID interviewId = UUID.randomUUID();
            String body = """
                    {
                      "interviewId": "%s"
                    }
                    """.formatted(interviewId);

            mockMvc.perform(post("/api/v1/ai/interview-summary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ai/suggestions")
    class GetSuggestions {

        @Test
        @DisplayName("should return paginated suggestions and 200")
        void getSuggestions_success() throws Exception {
            AiResponse response1 = buildAiResponse(AiSuggestionType.QUESTION_SUGGESTION);
            AiResponse response2 = buildAiResponse(AiSuggestionType.RESUME_PARSE);

            PaginatedResponse<AiResponse> paginatedResponse = PaginatedResponse.<AiResponse>builder()
                    .content(List.of(response1, response2))
                    .page(0)
                    .size(10)
                    .totalElements(2L)
                    .totalPages(1)
                    .last(true)
                    .build();

            given(aiService.getSuggestions(currentUserId, 0, 10)).willReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/ai/suggestions")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.last").value(true));

            verify(aiService).getSuggestions(currentUserId, 0, 10);
        }

        @Test
        @DisplayName("should return empty paginated response")
        void getSuggestions_empty() throws Exception {
            PaginatedResponse<AiResponse> emptyResponse = PaginatedResponse.<AiResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(10)
                    .totalElements(0L)
                    .totalPages(0)
                    .last(true)
                    .build();

            given(aiService.getSuggestions(currentUserId, 0, 10)).willReturn(emptyResponse);

            mockMvc.perform(get("/api/v1/ai/suggestions")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("should use default page and size when not provided")
        void getSuggestions_defaultParams() throws Exception {
            PaginatedResponse<AiResponse> response = PaginatedResponse.<AiResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(10)
                    .totalElements(0L)
                    .totalPages(0)
                    .last(true)
                    .build();

            given(aiService.getSuggestions(currentUserId, 0, 10)).willReturn(response);

            mockMvc.perform(get("/api/v1/ai/suggestions"))
                    .andExpect(status().isOk());

            verify(aiService).getSuggestions(currentUserId, 0, 10);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ai/suggestions/interview/{interviewId}")
    class GetSuggestionsByInterview {

        @Test
        @DisplayName("should return suggestions for interview and 200")
        void getSuggestionsByInterview_success() throws Exception {
            UUID interviewId = UUID.randomUUID();
            AiResponse response = buildAiResponse(AiSuggestionType.INTERVIEW_SUMMARY);
            given(aiService.getSuggestionsByInterview(interviewId)).willReturn(List.of(response));

            mockMvc.perform(get("/api/v1/ai/suggestions/interview/{interviewId}", interviewId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(response.getId().toString()))
                    .andExpect(jsonPath("$[0].type").value("INTERVIEW_SUMMARY"));

            verify(aiService).getSuggestionsByInterview(interviewId);
        }

        @Test
        @DisplayName("should return empty list when no suggestions for interview")
        void getSuggestionsByInterview_empty() throws Exception {
            UUID interviewId = UUID.randomUUID();
            given(aiService.getSuggestionsByInterview(interviewId)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/ai/suggestions/interview/{interviewId}", interviewId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/ai/suggestions/{id}/status")
    class UpdateSuggestionStatus {

        @Test
        @DisplayName("should accept suggestion and return 200")
        void updateStatus_accept() throws Exception {
            UUID suggestionId = UUID.randomUUID();
            AiResponse response = buildAiResponse(AiSuggestionType.QUESTION_SUGGESTION);
            response.setStatus(AiSuggestionStatus.ACCEPTED);
            given(aiService.updateStatus(suggestionId, "ACCEPTED")).willReturn(response);

            String body = """
                    {
                      "status": "ACCEPTED"
                    }
                    """;

            mockMvc.perform(patch("/api/v1/ai/suggestions/{id}/status", suggestionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));

            verify(aiService).updateStatus(suggestionId, "ACCEPTED");
        }

        @Test
        @DisplayName("should reject suggestion and return 200")
        void updateStatus_reject() throws Exception {
            UUID suggestionId = UUID.randomUUID();
            AiResponse response = buildAiResponse(AiSuggestionType.RESUME_PARSE);
            response.setStatus(AiSuggestionStatus.REJECTED);
            given(aiService.updateStatus(suggestionId, "REJECTED")).willReturn(response);

            String body = """
                    {
                      "status": "REJECTED"
                    }
                    """;

            mockMvc.perform(patch("/api/v1/ai/suggestions/{id}/status", suggestionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(aiService).updateStatus(suggestionId, "REJECTED");
        }

        @Test
        @DisplayName("should return 404 when suggestion not found")
        void updateStatus_notFound() throws Exception {
            UUID suggestionId = UUID.randomUUID();
            given(aiService.updateStatus(suggestionId, "ACCEPTED"))
                    .willThrow(new ResourceNotFoundException("AiSuggestion", "id", suggestionId));

            String body = """
                    {
                      "status": "ACCEPTED"
                    }
                    """;

            mockMvc.perform(patch("/api/v1/ai/suggestions/{id}/status", suggestionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 for invalid status")
        void updateStatus_invalidStatus() throws Exception {
            UUID suggestionId = UUID.randomUUID();
            given(aiService.updateStatus(suggestionId, "INVALID"))
                    .willThrow(new BadRequestException("Invalid status: INVALID. Must be ACCEPTED or REJECTED"));

            String body = """
                    {
                      "status": "INVALID"
                    }
                    """;

            mockMvc.perform(patch("/api/v1/ai/suggestions/{id}/status", suggestionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}

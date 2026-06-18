package com.interview_platform_backend.interview_platform_backend.candidatefeedback.controller;

import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.CandidateFeedbackResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.FeedbackSummaryResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.service.CandidateFeedbackService;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateFeedbackControllerWebMvcTest {

    private MockMvc mockMvc;
    private CandidateFeedbackService candidateFeedbackService;
    private SecurityHelper securityHelper;

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID INTERVIEW_ID = UUID.randomUUID();
    private static final UUID FEEDBACK_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        candidateFeedbackService = mock(CandidateFeedbackService.class);
        securityHelper = mock(SecurityHelper.class);
        CandidateFeedbackController controller = new CandidateFeedbackController(candidateFeedbackService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        given(securityHelper.getCurrentUserId()).willReturn(CURRENT_USER_ID);
    }

    private CandidateFeedbackResponse sampleResponse() {
        return CandidateFeedbackResponse.builder()
                .id(FEEDBACK_ID)
                .interviewId(INTERVIEW_ID)
                .candidateId(CURRENT_USER_ID)
                .candidateName("John Doe")
                .overallRating(4)
                .communicationRating(5)
                .professionalismRating(4)
                .technicalClarityRating(3)
                .timelinessRating(4)
                .comments("Great experience")
                .wouldRecommend(true)
                .isAnonymous(false)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/candidate-feedback")
    class SubmitFeedback {

        @Test
        @DisplayName("should submit feedback successfully")
        void submitFeedback_success() throws Exception {
            given(candidateFeedbackService.submitFeedback(any(), eq(CURRENT_USER_ID)))
                    .willReturn(sampleResponse());

            String body = """
                    {
                      "interviewId": "%s",
                      "overallRating": 4,
                      "communicationRating": 5,
                      "professionalismRating": 4,
                      "technicalClarityRating": 3,
                      "timelinessRating": 4,
                      "comments": "Great experience",
                      "wouldRecommend": true,
                      "isAnonymous": false
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/candidate-feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(FEEDBACK_ID.toString()))
                    .andExpect(jsonPath("$.interviewId").value(INTERVIEW_ID.toString()))
                    .andExpect(jsonPath("$.overallRating").value(4))
                    .andExpect(jsonPath("$.candidateName").value("John Doe"))
                    .andExpect(jsonPath("$.wouldRecommend").value(true));
        }

        @Test
        @DisplayName("should return 409 when duplicate feedback")
        void submitFeedback_duplicate_returnsConflict() throws Exception {
            given(candidateFeedbackService.submitFeedback(any(), eq(CURRENT_USER_ID)))
                    .willThrow(new DuplicateResourceException("Feedback already submitted for this interview by this candidate"));

            String body = """
                    {
                      "interviewId": "%s",
                      "overallRating": 4
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/candidate-feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Feedback already submitted for this interview by this candidate"));
        }

        @Test
        @DisplayName("should return 404 when interview not found")
        void submitFeedback_interviewNotFound_returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            given(candidateFeedbackService.submitFeedback(any(), eq(CURRENT_USER_ID)))
                    .willThrow(new ResourceNotFoundException("Interview", "id", unknownId));

            String body = """
                    {
                      "interviewId": "%s",
                      "overallRating": 4
                    }
                    """.formatted(unknownId);

            mockMvc.perform(post("/api/v1/candidate-feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when overallRating is missing")
        void submitFeedback_missingRating_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "interviewId": "%s"
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/candidate-feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when overallRating exceeds max")
        void submitFeedback_ratingTooHigh_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "interviewId": "%s",
                      "overallRating": 6
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/candidate-feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when overallRating is below min")
        void submitFeedback_ratingTooLow_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "interviewId": "%s",
                      "overallRating": 0
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/candidate-feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when interviewId is missing")
        void submitFeedback_missingInterviewId_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "overallRating": 4
                    }
                    """;

            mockMvc.perform(post("/api/v1/candidate-feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/candidate-feedback/interview/{interviewId}")
    class GetFeedbackByInterview {

        @Test
        @DisplayName("should return feedback list for interview")
        void getFeedbackByInterview_success() throws Exception {
            given(candidateFeedbackService.getFeedbackByInterview(INTERVIEW_ID))
                    .willReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/v1/candidate-feedback/interview/{interviewId}", INTERVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(FEEDBACK_ID.toString()))
                    .andExpect(jsonPath("$[0].overallRating").value(4));
        }

        @Test
        @DisplayName("should return empty list when no feedback")
        void getFeedbackByInterview_empty() throws Exception {
            given(candidateFeedbackService.getFeedbackByInterview(INTERVIEW_ID))
                    .willReturn(List.of());

            mockMvc.perform(get("/api/v1/candidate-feedback/interview/{interviewId}", INTERVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("should return 404 when interview not found")
        void getFeedbackByInterview_notFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            given(candidateFeedbackService.getFeedbackByInterview(unknownId))
                    .willThrow(new ResourceNotFoundException("Interview", "id", unknownId));

            mockMvc.perform(get("/api/v1/candidate-feedback/interview/{interviewId}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/candidate-feedback/summary")
    class GetFeedbackSummary {

        @Test
        @DisplayName("should return feedback summary")
        void getFeedbackSummary_success() throws Exception {
            FeedbackSummaryResponse summary = FeedbackSummaryResponse.builder()
                    .totalResponses(25L)
                    .averageOverallRating(4.2)
                    .averageCommunicationRating(4.0)
                    .averageProfessionalismRating(4.5)
                    .averageTechnicalClarityRating(3.8)
                    .averageTimelinessRating(4.1)
                    .recommendationRate(80.0)
                    .build();

            given(candidateFeedbackService.getFeedbackSummary()).willReturn(summary);

            mockMvc.perform(get("/api/v1/candidate-feedback/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResponses").value(25))
                    .andExpect(jsonPath("$.averageOverallRating").value(4.2))
                    .andExpect(jsonPath("$.averageCommunicationRating").value(4.0))
                    .andExpect(jsonPath("$.averageProfessionalismRating").value(4.5))
                    .andExpect(jsonPath("$.averageTechnicalClarityRating").value(3.8))
                    .andExpect(jsonPath("$.averageTimelinessRating").value(4.1))
                    .andExpect(jsonPath("$.recommendationRate").value(80.0));
        }

        @Test
        @DisplayName("should return zero summary when no data")
        void getFeedbackSummary_empty() throws Exception {
            FeedbackSummaryResponse summary = FeedbackSummaryResponse.builder()
                    .totalResponses(0L)
                    .averageOverallRating(0.0)
                    .averageCommunicationRating(0.0)
                    .averageProfessionalismRating(0.0)
                    .averageTechnicalClarityRating(0.0)
                    .averageTimelinessRating(0.0)
                    .recommendationRate(0.0)
                    .build();

            given(candidateFeedbackService.getFeedbackSummary()).willReturn(summary);

            mockMvc.perform(get("/api/v1/candidate-feedback/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResponses").value(0))
                    .andExpect(jsonPath("$.averageOverallRating").value(0.0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/candidate-feedback/my")
    class GetMyFeedback {

        @Test
        @DisplayName("should return paginated feedback for current user")
        void getMyFeedback_success() throws Exception {
            PaginatedResponse<CandidateFeedbackResponse> response = PaginatedResponse.<CandidateFeedbackResponse>builder()
                    .content(List.of(sampleResponse()))
                    .page(0)
                    .size(10)
                    .totalElements(1L)
                    .totalPages(1)
                    .last(true)
                    .build();

            given(candidateFeedbackService.getMyFeedback(CURRENT_USER_ID, 0, 10)).willReturn(response);

            mockMvc.perform(get("/api/v1/candidate-feedback/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(FEEDBACK_ID.toString()))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("should return empty page when no feedback")
        void getMyFeedback_empty() throws Exception {
            PaginatedResponse<CandidateFeedbackResponse> response = PaginatedResponse.<CandidateFeedbackResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(10)
                    .totalElements(0L)
                    .totalPages(0)
                    .last(true)
                    .build();

            given(candidateFeedbackService.getMyFeedback(CURRENT_USER_ID, 0, 10)).willReturn(response);

            mockMvc.perform(get("/api/v1/candidate-feedback/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("should use default page and size parameters")
        void getMyFeedback_defaultParams() throws Exception {
            PaginatedResponse<CandidateFeedbackResponse> response = PaginatedResponse.<CandidateFeedbackResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(10)
                    .totalElements(0L)
                    .totalPages(0)
                    .last(true)
                    .build();

            given(candidateFeedbackService.getMyFeedback(CURRENT_USER_ID, 0, 10)).willReturn(response);

            mockMvc.perform(get("/api/v1/candidate-feedback/my"))
                    .andExpect(status().isOk());
        }
    }
}

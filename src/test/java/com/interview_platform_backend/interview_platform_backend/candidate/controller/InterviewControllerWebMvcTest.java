package com.interview_platform_backend.interview_platform_backend.candidate.controller;

import com.interview_platform_backend.interview_platform_backend.candidate.dto.FeedbackResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.candidate.service.InterviewService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewControllerWebMvcTest {

    private MockMvc mockMvc;
    private InterviewService interviewService;
    private SecurityHelper securityHelper;

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID INTERVIEW_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID INTERVIEWER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        interviewService = mock(InterviewService.class);
        securityHelper = mock(SecurityHelper.class);
        InterviewController controller = new InterviewController(interviewService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        given(securityHelper.getCurrentUserId()).willReturn(CURRENT_USER_ID);
    }

    private InterviewResponse sampleResponse() {
        return InterviewResponse.builder()
                .id(INTERVIEW_ID)
                .title("Technical Interview")
                .description("Java backend assessment")
                .candidateId(CANDIDATE_ID)
                .candidateName("Cathy Candidate")
                .candidateEmail("cathy@example.com")
                .scheduledById(CURRENT_USER_ID)
                .scheduledByName("Ria Recruiter")
                .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .timeZone("Asia/Kolkata")
                .status(InterviewStatus.SCHEDULED)
                .type(InterviewType.TECHNICAL)
                .mode(InterviewMode.ONLINE)
                .meetingLink("https://meet.example.com/abc")
                .interviewers(List.of())
                .build();
    }

    // ==================== CRUD ====================

    @Test
    void createInterview_success_returnsOk() throws Exception {
        given(interviewService.createInterview(any(), eq(CURRENT_USER_ID)))
                .willReturn(sampleResponse());

        String startTime = Instant.now().plus(2, ChronoUnit.DAYS).toString();
        String endTime = Instant.now().plus(2, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS).toString();

        String body = """
                {
                  "title": "Technical Interview",
                  "description": "Java backend assessment",
                  "candidateId": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "timeZone": "Asia/Kolkata",
                  "type": "TECHNICAL",
                  "mode": "ONLINE",
                  "meetingLink": "https://meet.example.com/abc",
                  "interviewerIds": ["%s"]
                }
                """.formatted(CANDIDATE_ID, startTime, endTime, INTERVIEWER_ID);

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Technical Interview"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void createInterview_candidateNotFound_returns404() throws Exception {
        given(interviewService.createInterview(any(), eq(CURRENT_USER_ID)))
                .willThrow(new ResourceNotFoundException("Candidate", "id", CANDIDATE_ID));

        String startTime = Instant.now().plus(2, ChronoUnit.DAYS).toString();
        String endTime = Instant.now().plus(2, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS).toString();

        String body = """
                {
                  "title": "Interview",
                  "candidateId": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "type": "TECHNICAL",
                  "mode": "ONLINE",
                  "interviewerIds": ["%s"]
                }
                """.formatted(CANDIDATE_ID, startTime, endTime, INTERVIEWER_ID);

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void createInterview_endTimeBeforeStart_returnsBadRequest() throws Exception {
        given(interviewService.createInterview(any(), eq(CURRENT_USER_ID)))
                .willThrow(new BadRequestException("End time must be after start time"));

        String startTime = Instant.now().plus(2, ChronoUnit.DAYS).toString();
        String endTime = Instant.now().plus(2, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS).toString();

        String body = """
                {
                  "title": "Interview",
                  "candidateId": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "type": "TECHNICAL",
                  "mode": "ONLINE",
                  "interviewerIds": ["%s"]
                }
                """.formatted(CANDIDATE_ID, startTime, endTime, INTERVIEWER_ID);

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInterview_success_returnsOk() throws Exception {
        given(interviewService.getInterview(INTERVIEW_ID)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/interviews/{interviewId}", INTERVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERVIEW_ID.toString()))
                .andExpect(jsonPath("$.title").value("Technical Interview"));
    }

    @Test
    void getInterview_notFound_returns404() throws Exception {
        given(interviewService.getInterview(INTERVIEW_ID))
                .willThrow(new ResourceNotFoundException("Interview", "id", INTERVIEW_ID));

        mockMvc.perform(get("/api/v1/interviews/{interviewId}", INTERVIEW_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllInterviews_returnsOk() throws Exception {
        given(interviewService.getInterviews()).willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/interviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Technical Interview"));
    }

    @Test
    void getInterviewsPaginated_returnsOk() throws Exception {
        PaginatedResponse<InterviewResponse> page = PaginatedResponse.<InterviewResponse>builder()
                .content(List.of(sampleResponse()))
                .page(0).size(10).totalElements(1L).totalPages(1).last(true)
                .build();
        given(interviewService.getInterviewsPaginated(0, 10)).willReturn(page);

        mockMvc.perform(get("/api/v1/interviews/paginated")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateInterview_success_returnsOk() throws Exception {
        InterviewResponse updated = sampleResponse();
        updated.setTitle("Updated Title");
        given(interviewService.updateInterview(eq(INTERVIEW_ID), any())).willReturn(updated);

        mockMvc.perform(put("/api/v1/interviews/{interviewId}", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updateInterview_notFound_returns404() throws Exception {
        given(interviewService.updateInterview(eq(INTERVIEW_ID), any()))
                .willThrow(new ResourceNotFoundException("Interview", "id", INTERVIEW_ID));

        mockMvc.perform(put("/api/v1/interviews/{interviewId}", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "X" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateInterview_cancelledInterview_returnsBadRequest() throws Exception {
        given(interviewService.updateInterview(eq(INTERVIEW_ID), any()))
                .willThrow(new BadRequestException("Cannot update a cancelled interview"));

        mockMvc.perform(put("/api/v1/interviews/{interviewId}", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "X" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteInterview_success_returnsNoContent() throws Exception {
        doNothing().when(interviewService).deleteInterview(INTERVIEW_ID);

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}", INTERVIEW_ID))
                .andExpect(status().isNoContent());

        verify(interviewService).deleteInterview(INTERVIEW_ID);
    }

    @Test
    void deleteInterview_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Interview", "id", INTERVIEW_ID))
                .when(interviewService).deleteInterview(INTERVIEW_ID);

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}", INTERVIEW_ID))
                .andExpect(status().isNotFound());
    }

    // ==================== Status Management ====================

    @Test
    void cancelInterview_success_returnsOk() throws Exception {
        InterviewResponse cancelled = sampleResponse();
        cancelled.setStatus(InterviewStatus.CANCELLED);
        cancelled.setCancelReason("Schedule conflict");
        given(interviewService.cancelInterview(eq(INTERVIEW_ID), any())).willReturn(cancelled);

        mockMvc.perform(patch("/api/v1/interviews/{interviewId}/cancel", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Schedule conflict" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Schedule conflict"));
    }

    @Test
    void cancelInterview_alreadyCancelled_returnsBadRequest() throws Exception {
        given(interviewService.cancelInterview(eq(INTERVIEW_ID), any()))
                .willThrow(new BadRequestException("Interview is already cancelled"));

        mockMvc.perform(patch("/api/v1/interviews/{interviewId}/cancel", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "xyz" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelInterview_missingReason_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/interviews/{interviewId}/cancel", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_success_returnsOk() throws Exception {
        InterviewResponse completed = sampleResponse();
        completed.setStatus(InterviewStatus.COMPLETED);
        given(interviewService.updateStatus(INTERVIEW_ID, InterviewStatus.COMPLETED)).willReturn(completed);

        mockMvc.perform(patch("/api/v1/interviews/{interviewId}/status", INTERVIEW_ID)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updateStatus_cancelledInterview_returnsBadRequest() throws Exception {
        given(interviewService.updateStatus(INTERVIEW_ID, InterviewStatus.COMPLETED))
                .willThrow(new BadRequestException("Cannot change status of a cancelled interview"));

        mockMvc.perform(patch("/api/v1/interviews/{interviewId}/status", INTERVIEW_ID)
                        .param("status", "COMPLETED"))
                .andExpect(status().isBadRequest());
    }

    // ==================== My Interviews ====================

    @Test
    void getMyInterviewsAsCandidate_returnsOk() throws Exception {
        given(interviewService.getMyInterviewsAsCandidate(CURRENT_USER_ID))
                .willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/interviews/my/candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Technical Interview"));
    }

    @Test
    void getMyInterviewsAsInterviewer_returnsOk() throws Exception {
        given(interviewService.getMyInterviewsAsInterviewer(CURRENT_USER_ID))
                .willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/interviews/my/interviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Technical Interview"));
    }

    @Test
    void getMyInterviewsAsCandidatePaginated_returnsOk() throws Exception {
        PaginatedResponse<InterviewResponse> page = PaginatedResponse.<InterviewResponse>builder()
                .content(List.of(sampleResponse()))
                .page(0).size(10).totalElements(1L).totalPages(1).last(true)
                .build();
        given(interviewService.getMyInterviewsAsCandidatePaginated(CURRENT_USER_ID, 0, 10)).willReturn(page);

        mockMvc.perform(get("/api/v1/interviews/my/candidate/paginated")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMyInterviewsAsInterviewerPaginated_returnsOk() throws Exception {
        PaginatedResponse<InterviewResponse> page = PaginatedResponse.<InterviewResponse>builder()
                .content(List.of(sampleResponse()))
                .page(0).size(10).totalElements(1L).totalPages(1).last(true)
                .build();
        given(interviewService.getMyInterviewsAsInterviewerPaginated(CURRENT_USER_ID, 0, 10)).willReturn(page);

        mockMvc.perform(get("/api/v1/interviews/my/interviewer/paginated")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ==================== Interviewer Management ====================

    @Test
    void addInterviewer_success_returnsOk() throws Exception {
        given(interviewService.addInterviewer(INTERVIEW_ID, INTERVIEWER_ID, false))
                .willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/interviews/{interviewId}/interviewers/{interviewerId}",
                        INTERVIEW_ID, INTERVIEWER_ID)
                        .param("isPrimary", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void addInterviewer_duplicate_returnsConflict() throws Exception {
        given(interviewService.addInterviewer(INTERVIEW_ID, INTERVIEWER_ID, false))
                .willThrow(new DuplicateResourceException("Interviewer already assigned to this interview"));

        mockMvc.perform(post("/api/v1/interviews/{interviewId}/interviewers/{interviewerId}",
                        INTERVIEW_ID, INTERVIEWER_ID)
                        .param("isPrimary", "false"))
                .andExpect(status().isConflict());
    }

    @Test
    void addInterviewer_interviewerNotFound_returns404() throws Exception {
        given(interviewService.addInterviewer(INTERVIEW_ID, INTERVIEWER_ID, false))
                .willThrow(new ResourceNotFoundException("Interviewer", "id", INTERVIEWER_ID));

        mockMvc.perform(post("/api/v1/interviews/{interviewId}/interviewers/{interviewerId}",
                        INTERVIEW_ID, INTERVIEWER_ID)
                        .param("isPrimary", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeInterviewer_success_returnsOk() throws Exception {
        given(interviewService.removeInterviewer(INTERVIEW_ID, INTERVIEWER_ID))
                .willReturn(sampleResponse());

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}/interviewers/{interviewerId}",
                        INTERVIEW_ID, INTERVIEWER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void removeInterviewer_notAssigned_returns404() throws Exception {
        given(interviewService.removeInterviewer(INTERVIEW_ID, INTERVIEWER_ID))
                .willThrow(new ResourceNotFoundException("Interviewer assignment not found"));

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}/interviewers/{interviewerId}",
                        INTERVIEW_ID, INTERVIEWER_ID))
                .andExpect(status().isNotFound());
    }

    // ==================== Feedback ====================

    @Test
    void submitFeedback_success_returnsOk() throws Exception {
        FeedbackResponse feedback = FeedbackResponse.builder()
                .id(UUID.randomUUID())
                .interviewId(INTERVIEW_ID)
                .interviewerId(CURRENT_USER_ID)
                .interviewerName("Ian Interviewer")
                .rating(4)
                .recommendation(FeedbackRecommendation.HIRE)
                .strengths("Good problem solving")
                .build();

        given(interviewService.submitFeedback(eq(INTERVIEW_ID), eq(CURRENT_USER_ID), any()))
                .willReturn(feedback);

        mockMvc.perform(post("/api/v1/interviews/{interviewId}/feedback", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4,
                                  "recommendation": "HIRE",
                                  "strengths": "Good problem solving",
                                  "comments": "Strong candidate"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.recommendation").value("HIRE"));
    }

    @Test
    void submitFeedback_duplicateFeedback_returnsConflict() throws Exception {
        given(interviewService.submitFeedback(eq(INTERVIEW_ID), eq(CURRENT_USER_ID), any()))
                .willThrow(new DuplicateResourceException("Feedback already submitted for this interview by this interviewer"));

        mockMvc.perform(post("/api/v1/interviews/{interviewId}/feedback", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4,
                                  "recommendation": "HIRE"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void submitFeedback_interviewNotCompleted_returnsBadRequest() throws Exception {
        given(interviewService.submitFeedback(eq(INTERVIEW_ID), eq(CURRENT_USER_ID), any()))
                .willThrow(new BadRequestException("Feedback can only be submitted for completed or in-progress interviews"));

        mockMvc.perform(post("/api/v1/interviews/{interviewId}/feedback", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4,
                                  "recommendation": "HIRE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitFeedback_invalidRating_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/interviews/{interviewId}/feedback", INTERVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 10,
                                  "recommendation": "HIRE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInterviewFeedback_returnsOk() throws Exception {
        FeedbackResponse feedback = FeedbackResponse.builder()
                .id(UUID.randomUUID())
                .interviewId(INTERVIEW_ID)
                .rating(5)
                .recommendation(FeedbackRecommendation.HIRE)
                .build();

        given(interviewService.getInterviewFeedback(INTERVIEW_ID)).willReturn(List.of(feedback));

        mockMvc.perform(get("/api/v1/interviews/{interviewId}/feedback", INTERVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void getFeedbackByInterviewer_success_returnsOk() throws Exception {
        FeedbackResponse feedback = FeedbackResponse.builder()
                .id(UUID.randomUUID())
                .interviewId(INTERVIEW_ID)
                .interviewerId(INTERVIEWER_ID)
                .rating(3)
                .recommendation(FeedbackRecommendation.HOLD)
                .build();

        given(interviewService.getFeedbackByInterviewer(INTERVIEW_ID, INTERVIEWER_ID)).willReturn(feedback);

        mockMvc.perform(get("/api/v1/interviews/{interviewId}/feedback/interviewer/{interviewerId}",
                        INTERVIEW_ID, INTERVIEWER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(3));
    }

    @Test
    void getFeedbackByInterviewer_notFound_returns404() throws Exception {
        given(interviewService.getFeedbackByInterviewer(INTERVIEW_ID, INTERVIEWER_ID))
                .willThrow(new ResourceNotFoundException("Feedback", "interviewerId", INTERVIEWER_ID));

        mockMvc.perform(get("/api/v1/interviews/{interviewId}/feedback/interviewer/{interviewerId}",
                        INTERVIEW_ID, INTERVIEWER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllFeedbackByInterviewer_returnsOk() throws Exception {
        given(interviewService.getAllFeedbackByInterviewer(INTERVIEWER_ID)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/interviews/feedback/interviewer/{interviewerId}", INTERVIEWER_ID))
                .andExpect(status().isOk());
    }

    // ==================== Filters ====================

    @Test
    void getInterviewsByStatus_returnsOk() throws Exception {
        given(interviewService.getInterviewsByStatus(InterviewStatus.SCHEDULED))
                .willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/interviews/filter/status")
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void getInterviewsByStatusPaginated_returnsOk() throws Exception {
        PaginatedResponse<InterviewResponse> page = PaginatedResponse.<InterviewResponse>builder()
                .content(List.of(sampleResponse()))
                .page(0).size(10).totalElements(1L).totalPages(1).last(true)
                .build();
        given(interviewService.getInterviewsByStatusPaginated(InterviewStatus.SCHEDULED, 0, 10)).willReturn(page);

        mockMvc.perform(get("/api/v1/interviews/filter/status/paginated")
                        .param("status", "SCHEDULED")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getInterviewsByDateRange_returnsOk() throws Exception {
        Instant from = Instant.now();
        Instant to = Instant.now().plus(7, ChronoUnit.DAYS);

        given(interviewService.getInterviewsByDateRange(from, to)).willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/interviews/filter/date-range")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getInterviewsByDateRangePaginated_returnsOk() throws Exception {
        Instant from = Instant.now();
        Instant to = Instant.now().plus(7, ChronoUnit.DAYS);
        PaginatedResponse<InterviewResponse> page = PaginatedResponse.<InterviewResponse>builder()
                .content(List.of())
                .page(0).size(10).totalElements(0L).totalPages(0).last(true)
                .build();
        given(interviewService.getInterviewsByDateRangePaginated(from, to, 0, 10)).willReturn(page);

        mockMvc.perform(get("/api/v1/interviews/filter/date-range/paginated")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }
}



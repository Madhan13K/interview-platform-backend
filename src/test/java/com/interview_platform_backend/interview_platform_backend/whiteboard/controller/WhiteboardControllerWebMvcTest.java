package com.interview_platform_backend.interview_platform_backend.whiteboard.controller;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.WhiteboardSessionResponse;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.WhiteboardStrokeResponse;
import com.interview_platform_backend.interview_platform_backend.whiteboard.service.WhiteboardService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhiteboardControllerWebMvcTest {

    private MockMvc mockMvc;
    private WhiteboardService whiteboardService;
    private SecurityHelper securityHelper;

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID INTERVIEW_ID = UUID.randomUUID();
    private static final UUID STROKE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        whiteboardService = mock(WhiteboardService.class);
        securityHelper = mock(SecurityHelper.class);
        WhiteboardController controller = new WhiteboardController(whiteboardService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        given(securityHelper.getCurrentUserId()).willReturn(CURRENT_USER_ID);
    }

    private WhiteboardSessionResponse sampleSessionResponse() {
        return WhiteboardSessionResponse.builder()
                .id(SESSION_ID)
                .interviewId(INTERVIEW_ID)
                .createdById(CURRENT_USER_ID)
                .createdByName("Creator User")
                .title("Whiteboard Session")
                .isActive(true)
                .strokeCount(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private WhiteboardStrokeResponse sampleStrokeResponse() {
        return WhiteboardStrokeResponse.builder()
                .id(STROKE_ID)
                .sessionId(SESSION_ID)
                .userId(CURRENT_USER_ID)
                .userName("Creator User")
                .strokeData("{\"points\": [[0,0],[10,10]]}")
                .tool("PEN")
                .color("#FF0000")
                .strokeWidth(2.0)
                .sequenceNumber(1)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/whiteboards")
    class CreateSession {

        @Test
        @DisplayName("should create session and return 201")
        void createSession_success() throws Exception {
            given(whiteboardService.createSession(any(), eq(CURRENT_USER_ID)))
                    .willReturn(sampleSessionResponse());

            String body = """
                    {
                      "interviewId": "%s",
                      "title": "Algorithm Design"
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/whiteboards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.interviewId").value(INTERVIEW_ID.toString()))
                    .andExpect(jsonPath("$.createdByName").value("Creator User"))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @DisplayName("should return 404 when interview not found")
        void createSession_interviewNotFound() throws Exception {
            given(whiteboardService.createSession(any(), eq(CURRENT_USER_ID)))
                    .willThrow(new ResourceNotFoundException("Interview", "id", INTERVIEW_ID));

            String body = """
                    {
                      "interviewId": "%s"
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/whiteboards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when interviewId is missing")
        void createSession_missingInterviewId() throws Exception {
            mockMvc.perform(post("/api/v1/whiteboards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/whiteboards/{id}")
    class GetSession {

        @Test
        @DisplayName("should return session by ID")
        void getSession_success() throws Exception {
            given(whiteboardService.getSession(SESSION_ID)).willReturn(sampleSessionResponse());

            mockMvc.perform(get("/api/v1/whiteboards/{id}", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.title").value("Whiteboard Session"))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @DisplayName("should return 404 when session not found")
        void getSession_notFound() throws Exception {
            given(whiteboardService.getSession(SESSION_ID))
                    .willThrow(new ResourceNotFoundException("WhiteboardSession", "id", SESSION_ID));

            mockMvc.perform(get("/api/v1/whiteboards/{id}", SESSION_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/whiteboards/interview/{interviewId}")
    class GetSessionsByInterview {

        @Test
        @DisplayName("should return sessions for interview")
        void getSessionsByInterview_success() throws Exception {
            List<WhiteboardSessionResponse> sessions = List.of(
                    sampleSessionResponse(),
                    WhiteboardSessionResponse.builder()
                            .id(UUID.randomUUID())
                            .interviewId(INTERVIEW_ID)
                            .createdById(CURRENT_USER_ID)
                            .createdByName("Creator User")
                            .title("Second Session")
                            .isActive(true)
                            .strokeCount(5L)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );
            given(whiteboardService.getSessionsByInterview(INTERVIEW_ID)).willReturn(sessions);

            mockMvc.perform(get("/api/v1/whiteboards/interview/{interviewId}", INTERVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].interviewId").value(INTERVIEW_ID.toString()));
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void getSessionsByInterview_empty() throws Exception {
            given(whiteboardService.getSessionsByInterview(INTERVIEW_ID)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/whiteboards/interview/{interviewId}", INTERVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/whiteboards/{id}/strokes")
    class AddStroke {

        @Test
        @DisplayName("should add stroke and return 201")
        void addStroke_success() throws Exception {
            given(whiteboardService.addStroke(eq(SESSION_ID), any(), eq(CURRENT_USER_ID)))
                    .willReturn(sampleStrokeResponse());

            String body = """
                    {
                      "strokeData": "{\\"points\\": [[0,0],[10,10]]}",
                      "tool": "PEN",
                      "color": "#FF0000",
                      "strokeWidth": 2.0
                    }
                    """;

            mockMvc.perform(post("/api/v1/whiteboards/{id}/strokes", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(STROKE_ID.toString()))
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.tool").value("PEN"))
                    .andExpect(jsonPath("$.color").value("#FF0000"))
                    .andExpect(jsonPath("$.sequenceNumber").value(1));
        }

        @Test
        @DisplayName("should return 404 when session not found")
        void addStroke_sessionNotFound() throws Exception {
            given(whiteboardService.addStroke(eq(SESSION_ID), any(), eq(CURRENT_USER_ID)))
                    .willThrow(new ResourceNotFoundException("WhiteboardSession", "id", SESSION_ID));

            String body = """
                    {
                      "strokeData": "{\\"points\\": [[0,0]]}",
                      "tool": "PEN",
                      "color": "#000000",
                      "strokeWidth": 1.0
                    }
                    """;

            mockMvc.perform(post("/api/v1/whiteboards/{id}/strokes", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when session is closed")
        void addStroke_sessionClosed() throws Exception {
            given(whiteboardService.addStroke(eq(SESSION_ID), any(), eq(CURRENT_USER_ID)))
                    .willThrow(new BadRequestException("Cannot add stroke to a closed whiteboard session"));

            String body = """
                    {
                      "strokeData": "{\\"points\\": [[0,0]]}",
                      "tool": "PEN",
                      "color": "#000000",
                      "strokeWidth": 1.0
                    }
                    """;

            mockMvc.perform(post("/api/v1/whiteboards/{id}/strokes", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when strokeData is missing")
        void addStroke_missingStrokeData() throws Exception {
            mockMvc.perform(post("/api/v1/whiteboards/{id}/strokes", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tool": "PEN",
                                      "color": "#000000",
                                      "strokeWidth": 1.0
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/whiteboards/{id}/strokes")
    class GetStrokes {

        @Test
        @DisplayName("should return strokes for session")
        void getStrokes_success() throws Exception {
            List<WhiteboardStrokeResponse> strokes = List.of(
                    sampleStrokeResponse(),
                    WhiteboardStrokeResponse.builder()
                            .id(UUID.randomUUID())
                            .sessionId(SESSION_ID)
                            .userId(CURRENT_USER_ID)
                            .userName("Creator User")
                            .strokeData("{\"points\": [[20,20],[30,30]]}")
                            .tool("LINE")
                            .color("#0000FF")
                            .strokeWidth(3.0)
                            .sequenceNumber(2)
                            .createdAt(Instant.now())
                            .build()
            );
            given(whiteboardService.getStrokes(SESSION_ID)).willReturn(strokes);

            mockMvc.perform(get("/api/v1/whiteboards/{id}/strokes", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].sequenceNumber").value(1))
                    .andExpect(jsonPath("$[1].sequenceNumber").value(2));
        }

        @Test
        @DisplayName("should return 404 when session not found")
        void getStrokes_sessionNotFound() throws Exception {
            given(whiteboardService.getStrokes(SESSION_ID))
                    .willThrow(new ResourceNotFoundException("WhiteboardSession", "id", SESSION_ID));

            mockMvc.perform(get("/api/v1/whiteboards/{id}/strokes", SESSION_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return empty list when no strokes")
        void getStrokes_empty() throws Exception {
            given(whiteboardService.getStrokes(SESSION_ID)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/whiteboards/{id}/strokes", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/whiteboards/{id}/snapshot")
    class SaveSnapshot {

        @Test
        @DisplayName("should save snapshot and return 200")
        void saveSnapshot_success() throws Exception {
            WhiteboardSessionResponse response = sampleSessionResponse();
            response.setSnapshotData("{\"canvas\":\"base64data\"}");
            given(whiteboardService.saveSnapshot(eq(SESSION_ID), eq("{\"canvas\":\"base64data\"}")))
                    .willReturn(response);

            mockMvc.perform(put("/api/v1/whiteboards/{id}/snapshot", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "snapshotData": "{\\"canvas\\":\\"base64data\\"}" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.snapshotData").value("{\"canvas\":\"base64data\"}"));
        }

        @Test
        @DisplayName("should return 404 when session not found")
        void saveSnapshot_sessionNotFound() throws Exception {
            given(whiteboardService.saveSnapshot(eq(SESSION_ID), any()))
                    .willThrow(new ResourceNotFoundException("WhiteboardSession", "id", SESSION_ID));

            mockMvc.perform(put("/api/v1/whiteboards/{id}/snapshot", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "snapshotData": "data" }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/whiteboards/{id}/close")
    class CloseSession {

        @Test
        @DisplayName("should close session and return 200")
        void closeSession_success() throws Exception {
            WhiteboardSessionResponse response = sampleSessionResponse();
            response.setIsActive(false);
            given(whiteboardService.closeSession(SESSION_ID)).willReturn(response);

            mockMvc.perform(patch("/api/v1/whiteboards/{id}/close", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.isActive").value(false));
        }

        @Test
        @DisplayName("should return 404 when session not found")
        void closeSession_notFound() throws Exception {
            given(whiteboardService.closeSession(SESSION_ID))
                    .willThrow(new ResourceNotFoundException("WhiteboardSession", "id", SESSION_ID));

            mockMvc.perform(patch("/api/v1/whiteboards/{id}/close", SESSION_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/whiteboards/{id}")
    class DeleteSession {

        @Test
        @DisplayName("should delete session and return 204")
        void deleteSession_success() throws Exception {
            doNothing().when(whiteboardService).deleteSession(SESSION_ID);

            mockMvc.perform(delete("/api/v1/whiteboards/{id}", SESSION_ID))
                    .andExpect(status().isNoContent());

            verify(whiteboardService).deleteSession(SESSION_ID);
        }

        @Test
        @DisplayName("should return 404 when session not found")
        void deleteSession_notFound() throws Exception {
            doThrow(new ResourceNotFoundException("WhiteboardSession", "id", SESSION_ID))
                    .when(whiteboardService).deleteSession(SESSION_ID);

            mockMvc.perform(delete("/api/v1/whiteboards/{id}", SESSION_ID))
                    .andExpect(status().isNotFound());
        }
    }
}

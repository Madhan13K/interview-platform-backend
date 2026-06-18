package com.interview_platform_backend.interview_platform_backend.video.controller;

import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.video.dto.VideoRecordingResponse;
import com.interview_platform_backend.interview_platform_backend.video.service.VideoRecordingService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VideoRecordingControllerWebMvcTest {

    private MockMvc mockMvc;
    private VideoRecordingService videoRecordingService;
    private SecurityHelper securityHelper;

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID RECORDING_ID = UUID.randomUUID();
    private static final UUID INTERVIEW_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        videoRecordingService = mock(VideoRecordingService.class);
        securityHelper = mock(SecurityHelper.class);
        VideoRecordingController controller = new VideoRecordingController(videoRecordingService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        given(securityHelper.getCurrentUserId()).willReturn(CURRENT_USER_ID);
    }

    private VideoRecordingResponse sampleResponse() {
        return VideoRecordingResponse.builder()
                .id(RECORDING_ID)
                .interviewId(INTERVIEW_ID)
                .recordedByUserId(CURRENT_USER_ID)
                .recordedByName("Recorder User")
                .fileName("recording_abc12345_1700000000000.webm")
                .fileSizeBytes(1024000L)
                .durationSeconds(120)
                .mimeType("video/webm")
                .status("PROCESSING")
                .startedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/video-recordings/start")
    class StartRecording {

        @Test
        @DisplayName("should start recording and return 200")
        void startRecording_success() throws Exception {
            VideoRecordingResponse response = sampleResponse();
            given(videoRecordingService.startRecording(any(), eq(CURRENT_USER_ID))).willReturn(response);

            String body = """
                    {
                      "interviewId": "%s"
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/video-recordings/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(RECORDING_ID.toString()))
                    .andExpect(jsonPath("$.interviewId").value(INTERVIEW_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PROCESSING"))
                    .andExpect(jsonPath("$.recordedByName").value("Recorder User"));
        }

        @Test
        @DisplayName("should return 404 when interview not found")
        void startRecording_interviewNotFound() throws Exception {
            given(videoRecordingService.startRecording(any(), eq(CURRENT_USER_ID)))
                    .willThrow(new ResourceNotFoundException("Interview", "id", INTERVIEW_ID));

            String body = """
                    {
                      "interviewId": "%s"
                    }
                    """.formatted(INTERVIEW_ID);

            mockMvc.perform(post("/api/v1/video-recordings/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when interviewId is missing")
        void startRecording_missingInterviewId() throws Exception {
            mockMvc.perform(post("/api/v1/video-recordings/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/video-recordings/{id}/complete")
    class CompleteRecording {

        @Test
        @DisplayName("should complete recording and return 200")
        void completeRecording_success() throws Exception {
            VideoRecordingResponse response = sampleResponse();
            response.setStatus("READY");
            response.setFileSizeBytes(2048000L);
            response.setDurationSeconds(180);
            response.setDownloadUrl("https://s3.amazonaws.com/fake-presigned-url");
            given(videoRecordingService.completeRecording(eq(RECORDING_ID), eq(2048000L), eq(180)))
                    .willReturn(response);

            String body = """
                    {
                      "fileSizeBytes": 2048000,
                      "durationSeconds": 180
                    }
                    """;

            mockMvc.perform(patch("/api/v1/video-recordings/{id}/complete", RECORDING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("READY"))
                    .andExpect(jsonPath("$.fileSizeBytes").value(2048000))
                    .andExpect(jsonPath("$.durationSeconds").value(180))
                    .andExpect(jsonPath("$.downloadUrl").value("https://s3.amazonaws.com/fake-presigned-url"));
        }

        @Test
        @DisplayName("should return 404 when recording not found")
        void completeRecording_notFound() throws Exception {
            given(videoRecordingService.completeRecording(eq(RECORDING_ID), any(), any()))
                    .willThrow(new ResourceNotFoundException("VideoRecording", "id", RECORDING_ID));

            mockMvc.perform(patch("/api/v1/video-recordings/{id}/complete", RECORDING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "fileSizeBytes": 1024, "durationSeconds": 60 }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/video-recordings/{id}/fail")
    class FailRecording {

        @Test
        @DisplayName("should mark recording as failed and return 200")
        void failRecording_success() throws Exception {
            VideoRecordingResponse response = sampleResponse();
            response.setStatus("FAILED");
            response.setEndedAt(Instant.now());
            given(videoRecordingService.failRecording(eq(RECORDING_ID), eq("Upload timeout")))
                    .willReturn(response);

            mockMvc.perform(patch("/api/v1/video-recordings/{id}/fail", RECORDING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "reason": "Upload timeout" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"));
        }

        @Test
        @DisplayName("should return 404 when recording not found")
        void failRecording_notFound() throws Exception {
            given(videoRecordingService.failRecording(eq(RECORDING_ID), any()))
                    .willThrow(new ResourceNotFoundException("VideoRecording", "id", RECORDING_ID));

            mockMvc.perform(patch("/api/v1/video-recordings/{id}/fail", RECORDING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "reason": "Error" }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/video-recordings/interview/{interviewId}")
    class GetRecordingsByInterview {

        @Test
        @DisplayName("should return recordings list for interview")
        void getRecordingsByInterview_success() throws Exception {
            List<VideoRecordingResponse> recordings = List.of(sampleResponse(), sampleResponse());
            given(videoRecordingService.getRecordingsByInterview(INTERVIEW_ID)).willReturn(recordings);

            mockMvc.perform(get("/api/v1/video-recordings/interview/{interviewId}", INTERVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].interviewId").value(INTERVIEW_ID.toString()));
        }

        @Test
        @DisplayName("should return 404 when interview not found")
        void getRecordingsByInterview_interviewNotFound() throws Exception {
            given(videoRecordingService.getRecordingsByInterview(INTERVIEW_ID))
                    .willThrow(new ResourceNotFoundException("Interview", "id", INTERVIEW_ID));

            mockMvc.perform(get("/api/v1/video-recordings/interview/{interviewId}", INTERVIEW_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return empty list when no recordings")
        void getRecordingsByInterview_empty() throws Exception {
            given(videoRecordingService.getRecordingsByInterview(INTERVIEW_ID)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/video-recordings/interview/{interviewId}", INTERVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/video-recordings/{id}")
    class GetRecording {

        @Test
        @DisplayName("should return recording by ID")
        void getRecording_success() throws Exception {
            VideoRecordingResponse response = sampleResponse();
            given(videoRecordingService.getRecording(RECORDING_ID)).willReturn(response);

            mockMvc.perform(get("/api/v1/video-recordings/{id}", RECORDING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(RECORDING_ID.toString()))
                    .andExpect(jsonPath("$.mimeType").value("video/webm"));
        }

        @Test
        @DisplayName("should return 404 when recording not found")
        void getRecording_notFound() throws Exception {
            given(videoRecordingService.getRecording(RECORDING_ID))
                    .willThrow(new ResourceNotFoundException("VideoRecording", "id", RECORDING_ID));

            mockMvc.perform(get("/api/v1/video-recordings/{id}", RECORDING_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/video-recordings/{id}")
    class DeleteRecording {

        @Test
        @DisplayName("should soft delete recording and return 200")
        void deleteRecording_success() throws Exception {
            VideoRecordingResponse response = sampleResponse();
            response.setStatus("DELETED");
            given(videoRecordingService.deleteRecording(RECORDING_ID)).willReturn(response);

            mockMvc.perform(delete("/api/v1/video-recordings/{id}", RECORDING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DELETED"));
        }

        @Test
        @DisplayName("should return 404 when recording not found")
        void deleteRecording_notFound() throws Exception {
            given(videoRecordingService.deleteRecording(RECORDING_ID))
                    .willThrow(new ResourceNotFoundException("VideoRecording", "id", RECORDING_ID));

            mockMvc.perform(delete("/api/v1/video-recordings/{id}", RECORDING_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/video-recordings/my")
    class GetMyRecordings {

        @Test
        @DisplayName("should return paginated recordings for current user")
        void getMyRecordings_success() throws Exception {
            PaginatedResponse<VideoRecordingResponse> page = PaginatedResponse.<VideoRecordingResponse>builder()
                    .content(List.of(sampleResponse()))
                    .page(0)
                    .size(10)
                    .totalElements(1L)
                    .totalPages(1)
                    .last(true)
                    .build();
            given(videoRecordingService.getMyRecordings(CURRENT_USER_ID, 0, 10)).willReturn(page);

            mockMvc.perform(get("/api/v1/video-recordings/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("should return empty page when no recordings")
        void getMyRecordings_empty() throws Exception {
            PaginatedResponse<VideoRecordingResponse> page = PaginatedResponse.<VideoRecordingResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(10)
                    .totalElements(0L)
                    .totalPages(0)
                    .last(true)
                    .build();
            given(videoRecordingService.getMyRecordings(CURRENT_USER_ID, 0, 10)).willReturn(page);

            mockMvc.perform(get("/api/v1/video-recordings/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }
}

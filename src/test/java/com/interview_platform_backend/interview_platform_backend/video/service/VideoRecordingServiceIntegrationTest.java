package com.interview_platform_backend.interview_platform_backend.video.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.video.dto.StartRecordingRequest;
import com.interview_platform_backend.interview_platform_backend.video.dto.VideoRecordingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class VideoRecordingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VideoRecordingService videoRecordingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @MockitoBean
    private S3Presigner s3Presigner;

    private User recorder;
    private User candidate;
    private User scheduler;
    private Interview interview;

    @BeforeEach
    void setUp() throws Exception {
        // Configure S3Presigner mock to return a fake presigned URL
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://s3.amazonaws.com/fake-presigned-url"));
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presignedRequest);

        recorder = userRepository.save(User.builder()
                .firstName("Recorder")
                .lastName("User")
                .email("recorder-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        candidate = userRepository.save(User.builder()
                .firstName("Candidate")
                .lastName("User")
                .email("candidate-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        scheduler = userRepository.save(User.builder()
                .firstName("Scheduler")
                .lastName("User")
                .email("scheduler-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        interview = interviewRepository.save(Interview.builder()
                .title("Test Interview")
                .candidate(candidate)
                .scheduledBy(scheduler)
                .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .status(InterviewStatus.SCHEDULED)
                .type(InterviewType.TECHNICAL)
                .mode(InterviewMode.ONLINE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    private VideoRecordingResponse startTestRecording() {
        StartRecordingRequest request = StartRecordingRequest.builder()
                .interviewId(interview.getId())
                .build();
        return videoRecordingService.startRecording(request, recorder.getId());
    }

    @Nested
    @DisplayName("Start Recording")
    class StartRecording {

        @Test
        @DisplayName("should start recording successfully")
        void startRecording_success() {
            StartRecordingRequest request = StartRecordingRequest.builder()
                    .interviewId(interview.getId())
                    .build();

            VideoRecordingResponse response = videoRecordingService.startRecording(request, recorder.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getInterviewId()).isEqualTo(interview.getId());
            assertThat(response.getRecordedByUserId()).isEqualTo(recorder.getId());
            assertThat(response.getRecordedByName()).isEqualTo("Recorder User");
            assertThat(response.getStatus()).isEqualTo("PROCESSING");
            assertThat(response.getMimeType()).isEqualTo("video/webm");
            assertThat(response.getFileName()).isNotBlank();
            assertThat(response.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when interview not found")
        void startRecording_interviewNotFound() {
            StartRecordingRequest request = StartRecordingRequest.builder()
                    .interviewId(UUID.randomUUID())
                    .build();

            assertThatThrownBy(() -> videoRecordingService.startRecording(request, recorder.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Complete Recording")
    class CompleteRecording {

        @Test
        @DisplayName("should complete recording successfully")
        void completeRecording_success() {
            VideoRecordingResponse started = startTestRecording();

            VideoRecordingResponse response = videoRecordingService.completeRecording(
                    started.getId(), 1024000L, 120);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(started.getId());
            assertThat(response.getStatus()).isEqualTo("READY");
            assertThat(response.getFileSizeBytes()).isEqualTo(1024000L);
            assertThat(response.getDurationSeconds()).isEqualTo(120);
            assertThat(response.getEndedAt()).isNotNull();
            assertThat(response.getDownloadUrl()).isNotBlank();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when recording not found")
        void completeRecording_notFound() {
            assertThatThrownBy(() -> videoRecordingService.completeRecording(UUID.randomUUID(), 1024L, 60))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Fail Recording")
    class FailRecording {

        @Test
        @DisplayName("should mark recording as failed")
        void failRecording_success() {
            VideoRecordingResponse started = startTestRecording();

            VideoRecordingResponse response = videoRecordingService.failRecording(
                    started.getId(), "Network error during upload");

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(started.getId());
            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getEndedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Recordings By Interview")
    class GetRecordingsByInterview {

        @Test
        @DisplayName("should return recordings for interview")
        void getRecordingsByInterview_success() {
            startTestRecording();
            startTestRecording();

            List<VideoRecordingResponse> recordings = videoRecordingService
                    .getRecordingsByInterview(interview.getId());

            assertThat(recordings).hasSize(2);
            assertThat(recordings).allMatch(r -> r.getInterviewId().equals(interview.getId()));
        }

        @Test
        @DisplayName("should return empty list when no recordings exist")
        void getRecordingsByInterview_empty() {
            List<VideoRecordingResponse> recordings = videoRecordingService
                    .getRecordingsByInterview(interview.getId());

            assertThat(recordings).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Recording")
    class GetRecording {

        @Test
        @DisplayName("should get recording by ID")
        void getRecording_success() {
            VideoRecordingResponse started = startTestRecording();

            VideoRecordingResponse response = videoRecordingService.getRecording(started.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(started.getId());
            assertThat(response.getInterviewId()).isEqualTo(interview.getId());
            assertThat(response.getRecordedByUserId()).isEqualTo(recorder.getId());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when recording not found")
        void getRecording_notFound() {
            assertThatThrownBy(() -> videoRecordingService.getRecording(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Recording")
    class DeleteRecording {

        @Test
        @DisplayName("should soft delete recording")
        void deleteRecording_success() {
            VideoRecordingResponse started = startTestRecording();

            VideoRecordingResponse response = videoRecordingService.deleteRecording(started.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(started.getId());
            assertThat(response.getStatus()).isEqualTo("DELETED");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when recording not found")
        void deleteRecording_notFound() {
            assertThatThrownBy(() -> videoRecordingService.deleteRecording(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get My Recordings")
    class GetMyRecordings {

        @Test
        @DisplayName("should return paginated recordings for user")
        void getMyRecordings_paginated() {
            startTestRecording();
            startTestRecording();
            startTestRecording();

            PaginatedResponse<VideoRecordingResponse> response = videoRecordingService
                    .getMyRecordings(recorder.getId(), 0, 2);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getTotalElements()).isEqualTo(3L);
            assertThat(response.getTotalPages()).isEqualTo(2);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty page when no recordings exist")
        void getMyRecordings_empty() {
            PaginatedResponse<VideoRecordingResponse> response = videoRecordingService
                    .getMyRecordings(recorder.getId(), 0, 10);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0L);
        }
    }
}

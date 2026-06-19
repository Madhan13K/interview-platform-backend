package com.interview_platform_backend.interview_platform_backend.whiteboard.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.AddStrokeRequest;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.CreateWhiteboardRequest;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.WhiteboardSessionResponse;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.WhiteboardStrokeResponse;
import com.interview_platform_backend.interview_platform_backend.whiteboard.websocket.WhiteboardWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class WhiteboardServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WhiteboardService whiteboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private User creator;
    private User candidate;
    private User scheduler;
    private Interview interview;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(User.builder()
                .firstName("Creator")
                .lastName("User")
                .email("creator-" + UUID.randomUUID() + "@test.com")
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

    private WhiteboardSessionResponse createTestSession() {
        CreateWhiteboardRequest request = CreateWhiteboardRequest.builder()
                .interviewId(interview.getId())
                .build();
        return whiteboardService.createSession(request, creator.getId());
    }

    private WhiteboardSessionResponse createTestSessionWithTitle(String title) {
        CreateWhiteboardRequest request = CreateWhiteboardRequest.builder()
                .interviewId(interview.getId())
                .title(title)
                .build();
        return whiteboardService.createSession(request, creator.getId());
    }

    @Nested
    @DisplayName("Create Session")
    class CreateSession {

        @Test
        @DisplayName("should create session successfully with default title")
        void createSession_success() {
            CreateWhiteboardRequest request = CreateWhiteboardRequest.builder()
                    .interviewId(interview.getId())
                    .build();

            WhiteboardSessionResponse response = whiteboardService.createSession(request, creator.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getInterviewId()).isEqualTo(interview.getId());
            assertThat(response.getCreatedById()).isEqualTo(creator.getId());
            assertThat(response.getCreatedByName()).isEqualTo("Creator User");
            assertThat(response.getTitle()).isEqualTo("Whiteboard Session");
            assertThat(response.getIsActive()).isTrue();
            assertThat(response.getStrokeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should create session with custom title")
        void createSession_withTitle() {
            WhiteboardSessionResponse response = createTestSessionWithTitle("Algorithm Design");

            assertThat(response.getTitle()).isEqualTo("Algorithm Design");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when interview not found")
        void createSession_interviewNotFound() {
            CreateWhiteboardRequest request = CreateWhiteboardRequest.builder()
                    .interviewId(UUID.randomUUID())
                    .build();

            assertThatThrownBy(() -> whiteboardService.createSession(request, creator.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Session")
    class GetSession {

        @Test
        @DisplayName("should get session by ID")
        void getSession_success() {
            WhiteboardSessionResponse created = createTestSession();

            WhiteboardSessionResponse response = whiteboardService.getSession(created.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(created.getId());
            assertThat(response.getInterviewId()).isEqualTo(interview.getId());
            assertThat(response.getCreatedById()).isEqualTo(creator.getId());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void getSession_notFound() {
            assertThatThrownBy(() -> whiteboardService.getSession(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Sessions By Interview")
    class GetSessionsByInterview {

        @Test
        @DisplayName("should return sessions for interview")
        void getSessionsByInterview_success() {
            createTestSession();
            createTestSessionWithTitle("Second Session");

            List<WhiteboardSessionResponse> sessions = whiteboardService
                    .getSessionsByInterview(interview.getId());

            assertThat(sessions).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void getSessionsByInterview_empty() {
            List<WhiteboardSessionResponse> sessions = whiteboardService
                    .getSessionsByInterview(interview.getId());

            assertThat(sessions).isEmpty();
        }
    }

    @Nested
    @DisplayName("Add Stroke")
    class AddStroke {

        @Test
        @DisplayName("should add stroke successfully")
        void addStroke_success() {
            WhiteboardSessionResponse session = createTestSession();

            AddStrokeRequest request = AddStrokeRequest.builder()
                    .strokeData("{\"points\": [[0,0],[10,10]]}")
                    .tool("PEN")
                    .color("#FF0000")
                    .strokeWidth(2.0)
                    .build();

            WhiteboardStrokeResponse response = whiteboardService.addStroke(
                    session.getId(), request, creator.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getSessionId()).isEqualTo(session.getId());
            assertThat(response.getUserId()).isEqualTo(creator.getId());
            assertThat(response.getUserName()).isEqualTo("Creator User");
            assertThat(response.getStrokeData()).isEqualTo("{\"points\": [[0,0],[10,10]]}");
            assertThat(response.getTool()).isEqualTo("PEN");
            assertThat(response.getColor()).isEqualTo("#FF0000");
            assertThat(response.getStrokeWidth()).isEqualTo(2.0);
            assertThat(response.getSequenceNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment sequence number for multiple strokes")
        void addStroke_incrementsSequence() {
            WhiteboardSessionResponse session = createTestSession();

            AddStrokeRequest request1 = AddStrokeRequest.builder()
                    .strokeData("{\"points\": [[0,0],[10,10]]}")
                    .tool("PEN")
                    .color("#FF0000")
                    .strokeWidth(2.0)
                    .build();

            AddStrokeRequest request2 = AddStrokeRequest.builder()
                    .strokeData("{\"points\": [[20,20],[30,30]]}")
                    .tool("LINE")
                    .color("#0000FF")
                    .strokeWidth(3.0)
                    .build();

            AddStrokeRequest request3 = AddStrokeRequest.builder()
                    .strokeData("{\"points\": [[40,40],[50,50]]}")
                    .tool("ERASER")
                    .color(null)
                    .strokeWidth(5.0)
                    .build();

            WhiteboardStrokeResponse stroke1 = whiteboardService.addStroke(session.getId(), request1, creator.getId());
            WhiteboardStrokeResponse stroke2 = whiteboardService.addStroke(session.getId(), request2, creator.getId());
            WhiteboardStrokeResponse stroke3 = whiteboardService.addStroke(session.getId(), request3, creator.getId());

            assertThat(stroke1.getSequenceNumber()).isEqualTo(1);
            assertThat(stroke2.getSequenceNumber()).isEqualTo(2);
            assertThat(stroke3.getSequenceNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void addStroke_sessionNotFound() {
            AddStrokeRequest request = AddStrokeRequest.builder()
                    .strokeData("{\"points\": [[0,0]]}")
                    .tool("PEN")
                    .color("#000000")
                    .strokeWidth(1.0)
                    .build();

            assertThatThrownBy(() -> whiteboardService.addStroke(UUID.randomUUID(), request, creator.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException when session is closed")
        void addStroke_sessionClosed() {
            WhiteboardSessionResponse session = createTestSession();
            whiteboardService.closeSession(session.getId());

            AddStrokeRequest request = AddStrokeRequest.builder()
                    .strokeData("{\"points\": [[0,0]]}")
                    .tool("PEN")
                    .color("#000000")
                    .strokeWidth(1.0)
                    .build();

            assertThatThrownBy(() -> whiteboardService.addStroke(session.getId(), request, creator.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("closed");
        }
    }

    @Nested
    @DisplayName("Get Strokes")
    class GetStrokes {

        @Test
        @DisplayName("should return strokes ordered by sequence number")
        void getStrokes_orderedBySequence() {
            WhiteboardSessionResponse session = createTestSession();

            whiteboardService.addStroke(session.getId(),
                    AddStrokeRequest.builder().strokeData("{\"a\":1}").tool("PEN").color("#000").strokeWidth(1.0).build(),
                    creator.getId());
            whiteboardService.addStroke(session.getId(),
                    AddStrokeRequest.builder().strokeData("{\"a\":2}").tool("LINE").color("#111").strokeWidth(2.0).build(),
                    creator.getId());
            whiteboardService.addStroke(session.getId(),
                    AddStrokeRequest.builder().strokeData("{\"a\":3}").tool("CIRCLE").color("#222").strokeWidth(3.0).build(),
                    creator.getId());

            List<WhiteboardStrokeResponse> strokes = whiteboardService.getStrokes(session.getId());

            assertThat(strokes).hasSize(3);
            assertThat(strokes.get(0).getSequenceNumber()).isEqualTo(1);
            assertThat(strokes.get(1).getSequenceNumber()).isEqualTo(2);
            assertThat(strokes.get(2).getSequenceNumber()).isEqualTo(3);
            assertThat(strokes.get(0).getStrokeData()).isEqualTo("{\"a\":1}");
            assertThat(strokes.get(1).getStrokeData()).isEqualTo("{\"a\":2}");
            assertThat(strokes.get(2).getStrokeData()).isEqualTo("{\"a\":3}");
        }

        @Test
        @DisplayName("should return empty list when no strokes")
        void getStrokes_empty() {
            WhiteboardSessionResponse session = createTestSession();

            List<WhiteboardStrokeResponse> strokes = whiteboardService.getStrokes(session.getId());

            assertThat(strokes).isEmpty();
        }
    }

    @Nested
    @DisplayName("Save Snapshot")
    class SaveSnapshot {

        @Test
        @DisplayName("should save snapshot data")
        void saveSnapshot_success() {
            WhiteboardSessionResponse session = createTestSession();

            String snapshotData = "{\"canvas\":\"data:image/png;base64,iVBOR...\"}";
            WhiteboardSessionResponse response = whiteboardService.saveSnapshot(session.getId(), snapshotData);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(session.getId());
            assertThat(response.getSnapshotData()).isEqualTo(snapshotData);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void saveSnapshot_sessionNotFound() {
            assertThatThrownBy(() -> whiteboardService.saveSnapshot(UUID.randomUUID(), "data"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Close Session")
    class CloseSession {

        @Test
        @DisplayName("should close session successfully")
        void closeSession_success() {
            WhiteboardSessionResponse session = createTestSession();
            assertThat(session.getIsActive()).isTrue();

            WhiteboardSessionResponse response = whiteboardService.closeSession(session.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(session.getId());
            assertThat(response.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should allow closing an already closed session")
        void closeSession_alreadyClosed() {
            WhiteboardSessionResponse session = createTestSession();
            whiteboardService.closeSession(session.getId());

            WhiteboardSessionResponse response = whiteboardService.closeSession(session.getId());

            assertThat(response.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void closeSession_notFound() {
            assertThatThrownBy(() -> whiteboardService.closeSession(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Session")
    class DeleteSession {

        @Test
        @DisplayName("should delete session and its strokes")
        void deleteSession_success() {
            WhiteboardSessionResponse session = createTestSession();

            // Add some strokes first
            whiteboardService.addStroke(session.getId(),
                    AddStrokeRequest.builder().strokeData("{\"x\":1}").tool("PEN").color("#000").strokeWidth(1.0).build(),
                    creator.getId());

            whiteboardService.deleteSession(session.getId());

            assertThatThrownBy(() -> whiteboardService.getSession(session.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void deleteSession_notFound() {
            assertThatThrownBy(() -> whiteboardService.deleteSession(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

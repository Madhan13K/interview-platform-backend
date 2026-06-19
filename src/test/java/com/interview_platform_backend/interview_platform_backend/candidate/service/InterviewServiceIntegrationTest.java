package com.interview_platform_backend.interview_platform_backend.candidate.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.candidate.dto.*;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.*;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class InterviewServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserRepository userRepository;

    private User candidate;
    private User interviewer;
    private User scheduler;

    @BeforeEach
    void setUp() {
        candidate = userRepository.save(User.builder()
                .firstName("Candidate")
                .lastName("User")
                .email("candidate-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        interviewer = userRepository.save(User.builder()
                .firstName("Interviewer")
                .lastName("User")
                .email("interviewer-" + UUID.randomUUID() + "@test.com")
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
    }

    private CreateInterviewRequest buildCreateRequest() {
        return CreateInterviewRequest.builder()
                .title("Technical Interview")
                .description("Java backend interview")
                .candidateId(candidate.getId())
                .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .timeZone("Asia/Kolkata")
                .type(InterviewType.TECHNICAL)
                .mode(InterviewMode.ONLINE)
                .meetingLink("https://meet.google.com/test")
                .interviewerIds(List.of(interviewer.getId()))
                .build();
    }

    @Nested
    @DisplayName("Create Interview")
    class CreateInterview {

        @Test
        @DisplayName("should create interview successfully")
        void createInterview_success() {
            InterviewResponse response = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Technical Interview");
            assertThat(response.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
            assertThat(response.getCandidateId()).isEqualTo(candidate.getId());
            assertThat(response.getScheduledById()).isEqualTo(scheduler.getId());
            assertThat(response.getInterviewers()).hasSize(1);
        }

        @Test
        @DisplayName("should throw BadRequestException when end time before start time")
        void createInterview_invalidTime() {
            CreateInterviewRequest request = buildCreateRequest();
            request.setEndTime(request.getStartTime().minus(1, ChronoUnit.HOURS));

            assertThatThrownBy(() -> interviewService.createInterview(request, scheduler.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("End time must be after start time");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid candidate")
        void createInterview_invalidCandidate() {
            CreateInterviewRequest request = buildCreateRequest();
            request.setCandidateId(UUID.randomUUID());

            assertThatThrownBy(() -> interviewService.createInterview(request, scheduler.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid scheduler")
        void createInterview_invalidScheduler() {
            assertThatThrownBy(() -> interviewService.createInterview(buildCreateRequest(), UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Interview")
    class GetInterview {

        @Test
        @DisplayName("should get interview by ID")
        void getInterview_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            InterviewResponse found = interviewService.getInterview(created.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getTitle()).isEqualTo("Technical Interview");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void getInterview_notFound() {
            assertThatThrownBy(() -> interviewService.getInterview(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should get all interviews")
        void getInterviews_success() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            List<InterviewResponse> interviews = interviewService.getInterviews();

            assertThat(interviews).isNotEmpty();
        }

        @Test
        @DisplayName("should get paginated interviews")
        void getInterviewsPaginated_success() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            PaginatedResponse<InterviewResponse> result = interviewService.getInterviewsPaginated(0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getPage()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Update Interview")
    class UpdateInterview {

        @Test
        @DisplayName("should update interview title")
        void updateInterview_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            UpdateInterviewRequest updateRequest = new UpdateInterviewRequest();
            updateRequest.setTitle("Updated Title");
            updateRequest.setDescription("Updated description");

            InterviewResponse updated = interviewService.updateInterview(created.getId(), updateRequest);

            assertThat(updated.getTitle()).isEqualTo("Updated Title");
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("should reschedule interview when times change")
        void updateInterview_reschedule() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            Instant newStart = Instant.now().plus(2, ChronoUnit.DAYS);
            Instant newEnd = newStart.plus(1, ChronoUnit.HOURS);

            UpdateInterviewRequest updateRequest = new UpdateInterviewRequest();
            updateRequest.setStartTime(newStart);
            updateRequest.setEndTime(newEnd);

            InterviewResponse updated = interviewService.updateInterview(created.getId(), updateRequest);

            assertThat(updated.getStatus()).isEqualTo(InterviewStatus.RESCHEDULED);
            assertThat(updated.getStartTime()).isEqualTo(newStart);
            assertThat(updated.getEndTime()).isEqualTo(newEnd);
        }

        @Test
        @DisplayName("should throw BadRequestException when updating cancelled interview")
        void updateInterview_cancelled() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.cancelInterview(created.getId(),
                    CancelInterviewRequest.builder().reason("Test cancel").build());

            UpdateInterviewRequest updateRequest = new UpdateInterviewRequest();
            updateRequest.setTitle("Should fail");

            assertThatThrownBy(() -> interviewService.updateInterview(created.getId(), updateRequest))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void updateInterview_notFound() {
            UpdateInterviewRequest updateRequest = new UpdateInterviewRequest();
            updateRequest.setTitle("Should fail");

            assertThatThrownBy(() -> interviewService.updateInterview(UUID.randomUUID(), updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Cancel Interview")
    class CancelInterview {

        @Test
        @DisplayName("should cancel interview successfully")
        void cancelInterview_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            CancelInterviewRequest cancelRequest = CancelInterviewRequest.builder()
                    .reason("Schedule conflict")
                    .build();

            InterviewResponse cancelled = interviewService.cancelInterview(created.getId(), cancelRequest);

            assertThat(cancelled.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
            assertThat(cancelled.getCancelReason()).isEqualTo("Schedule conflict");
        }

        @Test
        @DisplayName("should throw BadRequestException when already cancelled")
        void cancelInterview_alreadyCancelled() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.cancelInterview(created.getId(),
                    CancelInterviewRequest.builder().reason("First cancel").build());

            CancelInterviewRequest secondCancel = CancelInterviewRequest.builder()
                    .reason("Second cancel")
                    .build();

            assertThatThrownBy(() -> interviewService.cancelInterview(created.getId(), secondCancel))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already cancelled");
        }
    }

    @Nested
    @DisplayName("Update Status")
    class UpdateStatus {

        @Test
        @DisplayName("should update interview status")
        void updateStatus_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            InterviewResponse updated = interviewService.updateStatus(created.getId(), InterviewStatus.IN_PROGRESS);

            assertThat(updated.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should throw BadRequestException when updating cancelled interview status")
        void updateStatus_cancelled() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.cancelInterview(created.getId(),
                    CancelInterviewRequest.builder().reason("Cancel").build());

            assertThatThrownBy(() -> interviewService.updateStatus(created.getId(), InterviewStatus.IN_PROGRESS))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Delete Interview")
    class DeleteInterview {

        @Test
        @DisplayName("should delete interview successfully")
        void deleteInterview_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.deleteInterview(created.getId());

            assertThatThrownBy(() -> interviewService.getInterview(created.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void deleteInterview_notFound() {
            assertThatThrownBy(() -> interviewService.deleteInterview(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("My Interviews")
    class MyInterviews {

        @Test
        @DisplayName("should get interviews as candidate")
        void getMyInterviewsAsCandidate() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            List<InterviewResponse> interviews = interviewService.getMyInterviewsAsCandidate(candidate.getId());

            assertThat(interviews).isNotEmpty();
            assertThat(interviews).allMatch(i -> i.getCandidateId().equals(candidate.getId()));
        }

        @Test
        @DisplayName("should get interviews as interviewer")
        void getMyInterviewsAsInterviewer() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            List<InterviewResponse> interviews = interviewService.getMyInterviewsAsInterviewer(interviewer.getId());

            assertThat(interviews).isNotEmpty();
        }

        @Test
        @DisplayName("should get paginated interviews as candidate")
        void getMyInterviewsAsCandidatePaginated() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            PaginatedResponse<InterviewResponse> result =
                    interviewService.getMyInterviewsAsCandidatePaginated(candidate.getId(), 0, 10);

            assertThat(result.getContent()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Interviewer Management")
    class InterviewerManagement {

        @Test
        @DisplayName("should add interviewer to interview")
        void addInterviewer_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            User newInterviewer = userRepository.save(User.builder()
                    .firstName("New")
                    .lastName("Interviewer")
                    .email("new-interviewer-" + UUID.randomUUID() + "@test.com")
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build());

            InterviewResponse updated = interviewService.addInterviewer(
                    created.getId(), newInterviewer.getId(), false);

            assertThat(updated.getInterviewers()).hasSize(2);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when interviewer already assigned")
        void addInterviewer_duplicate() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            assertThatThrownBy(() ->
                    interviewService.addInterviewer(created.getId(), interviewer.getId(), false))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should remove interviewer from interview")
        void removeInterviewer_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            InterviewResponse updated = interviewService.removeInterviewer(
                    created.getId(), interviewer.getId());

            assertThat(updated.getInterviewers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Feedback")
    class Feedback {

        @Test
        @DisplayName("should submit feedback successfully")
        void submitFeedback_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            // Move to COMPLETED status first
            interviewService.updateStatus(created.getId(), InterviewStatus.COMPLETED);

            SubmitFeedbackRequest feedbackRequest = SubmitFeedbackRequest.builder()
                    .rating(4)
                    .recommendation(FeedbackRecommendation.HIRE)
                    .strengths("Good problem solving")
                    .weaknesses("Could improve communication")
                    .comments("Overall good candidate")
                    .build();

            FeedbackResponse feedback = interviewService.submitFeedback(
                    created.getId(), interviewer.getId(), feedbackRequest);

            assertThat(feedback).isNotNull();
            assertThat(feedback.getRating()).isEqualTo(4);
            assertThat(feedback.getRecommendation()).isEqualTo(FeedbackRecommendation.HIRE);
            assertThat(feedback.getInterviewerId()).isEqualTo(interviewer.getId());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate feedback")
        void submitFeedback_duplicate() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.updateStatus(created.getId(), InterviewStatus.COMPLETED);

            SubmitFeedbackRequest feedbackRequest = SubmitFeedbackRequest.builder()
                    .rating(4)
                    .recommendation(FeedbackRecommendation.HIRE)
                    .strengths("Good")
                    .build();

            interviewService.submitFeedback(created.getId(), interviewer.getId(), feedbackRequest);

            assertThatThrownBy(() ->
                    interviewService.submitFeedback(created.getId(), interviewer.getId(), feedbackRequest))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException for scheduled interview feedback")
        void submitFeedback_invalidStatus() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            SubmitFeedbackRequest feedbackRequest = SubmitFeedbackRequest.builder()
                    .rating(4)
                    .recommendation(FeedbackRecommendation.HIRE)
                    .build();

            assertThatThrownBy(() ->
                    interviewService.submitFeedback(created.getId(), interviewer.getId(), feedbackRequest))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should get interview feedback")
        void getInterviewFeedback_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.updateStatus(created.getId(), InterviewStatus.COMPLETED);

            interviewService.submitFeedback(created.getId(), interviewer.getId(),
                    SubmitFeedbackRequest.builder()
                            .rating(5)
                            .recommendation(FeedbackRecommendation.HIRE)
                            .build());

            List<FeedbackResponse> feedbacks = interviewService.getInterviewFeedback(created.getId());

            assertThat(feedbacks).hasSize(1);
            assertThat(feedbacks.get(0).getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("should get feedback by interviewer")
        void getFeedbackByInterviewer_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.updateStatus(created.getId(), InterviewStatus.COMPLETED);

            interviewService.submitFeedback(created.getId(), interviewer.getId(),
                    SubmitFeedbackRequest.builder()
                            .rating(3)
                            .recommendation(FeedbackRecommendation.HOLD)
                            .build());

            FeedbackResponse feedback = interviewService.getFeedbackByInterviewer(
                    created.getId(), interviewer.getId());

            assertThat(feedback).isNotNull();
            assertThat(feedback.getRating()).isEqualTo(3);
        }

        @Test
        @DisplayName("should get all feedback by interviewer")
        void getAllFeedbackByInterviewer_success() {
            InterviewResponse created = interviewService.createInterview(buildCreateRequest(), scheduler.getId());
            interviewService.updateStatus(created.getId(), InterviewStatus.COMPLETED);

            interviewService.submitFeedback(created.getId(), interviewer.getId(),
                    SubmitFeedbackRequest.builder()
                            .rating(4)
                            .recommendation(FeedbackRecommendation.HIRE)
                            .build());

            List<FeedbackResponse> feedbacks = interviewService.getAllFeedbackByInterviewer(interviewer.getId());

            assertThat(feedbacks).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Filter Interviews")
    class FilterInterviews {

        @Test
        @DisplayName("should filter by status")
        void getInterviewsByStatus() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            List<InterviewResponse> scheduled = interviewService.getInterviewsByStatus(InterviewStatus.SCHEDULED);

            assertThat(scheduled).isNotEmpty();
            assertThat(scheduled).allMatch(i -> i.getStatus() == InterviewStatus.SCHEDULED);
        }

        @Test
        @DisplayName("should filter by status paginated")
        void getInterviewsByStatusPaginated() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            PaginatedResponse<InterviewResponse> result =
                    interviewService.getInterviewsByStatusPaginated(InterviewStatus.SCHEDULED, 0, 10);

            assertThat(result.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("should filter by date range")
        void getInterviewsByDateRange() {
            interviewService.createInterview(buildCreateRequest(), scheduler.getId());

            Instant from = Instant.now();
            Instant to = Instant.now().plus(7, ChronoUnit.DAYS);

            List<InterviewResponse> interviews = interviewService.getInterviewsByDateRange(from, to);

            assertThat(interviews).isNotEmpty();
        }
    }
}


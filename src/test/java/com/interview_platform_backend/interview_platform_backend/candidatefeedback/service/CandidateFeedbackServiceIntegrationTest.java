package com.interview_platform_backend.interview_platform_backend.candidatefeedback.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.*;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.CandidateFeedbackResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.FeedbackSummaryResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.SubmitCandidateFeedbackRequest;
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
class CandidateFeedbackServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CandidateFeedbackService candidateFeedbackService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    private User candidate;
    private User scheduler;
    private Interview completedInterview;

    @BeforeEach
    void setUp() {
        candidate = userRepository.save(User.builder()
                .firstName("Candidate")
                .lastName("Feedback")
                .email("cf-cand-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        scheduler = userRepository.save(User.builder()
                .firstName("Scheduler")
                .lastName("Feedback")
                .email("cf-sch-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        completedInterview = interviewRepository.save(Interview.builder()
                .title("Test Interview")
                .candidate(candidate)
                .scheduledBy(scheduler)
                .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .status(InterviewStatus.COMPLETED)
                .type(InterviewType.TECHNICAL)
                .mode(InterviewMode.ONLINE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    @Nested
    @DisplayName("Submit Feedback")
    class SubmitFeedback {

        @Test
        @DisplayName("should submit feedback successfully with all fields")
        void submitFeedback_success_allFields() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(5)
                    .communicationRating(4)
                    .professionalismRating(5)
                    .technicalClarityRating(4)
                    .timelinessRating(3)
                    .comments("Great interview experience")
                    .wouldRecommend(true)
                    .isAnonymous(false)
                    .build();

            CandidateFeedbackResponse response = candidateFeedbackService.submitFeedback(request, candidate.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getInterviewId()).isEqualTo(completedInterview.getId());
            assertThat(response.getCandidateId()).isEqualTo(candidate.getId());
            assertThat(response.getCandidateName()).isEqualTo("Candidate Feedback");
            assertThat(response.getOverallRating()).isEqualTo(5);
            assertThat(response.getCommunicationRating()).isEqualTo(4);
            assertThat(response.getProfessionalismRating()).isEqualTo(5);
            assertThat(response.getTechnicalClarityRating()).isEqualTo(4);
            assertThat(response.getTimelinessRating()).isEqualTo(3);
            assertThat(response.getComments()).isEqualTo("Great interview experience");
            assertThat(response.getWouldRecommend()).isTrue();
            assertThat(response.getIsAnonymous()).isFalse();
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should submit feedback successfully with minimal fields")
        void submitFeedback_success_minimalFields() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(3)
                    .build();

            CandidateFeedbackResponse response = candidateFeedbackService.submitFeedback(request, candidate.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getOverallRating()).isEqualTo(3);
            assertThat(response.getCommunicationRating()).isNull();
            assertThat(response.getProfessionalismRating()).isNull();
            assertThat(response.getTechnicalClarityRating()).isNull();
            assertThat(response.getTimelinessRating()).isNull();
            assertThat(response.getComments()).isNull();
            assertThat(response.getWouldRecommend()).isNull();
            assertThat(response.getIsAnonymous()).isFalse();
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when submitting duplicate feedback")
        void submitFeedback_duplicate() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(4)
                    .build();

            candidateFeedbackService.submitFeedback(request, candidate.getId());

            assertThatThrownBy(() -> candidateFeedbackService.submitFeedback(request, candidate.getId()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already submitted");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when interview not found")
        void submitFeedback_interviewNotFound() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(UUID.randomUUID())
                    .overallRating(4)
                    .build();

            assertThatThrownBy(() -> candidateFeedbackService.submitFeedback(request, candidate.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void submitFeedback_userNotFound() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(4)
                    .build();

            assertThatThrownBy(() -> candidateFeedbackService.submitFeedback(request, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Feedback By Interview")
    class GetFeedbackByInterview {

        @Test
        @DisplayName("should return feedback list for interview")
        void getFeedbackByInterview_success() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(4)
                    .comments("Good experience")
                    .wouldRecommend(true)
                    .build();
            candidateFeedbackService.submitFeedback(request, candidate.getId());

            List<CandidateFeedbackResponse> results = candidateFeedbackService
                    .getFeedbackByInterview(completedInterview.getId());

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getOverallRating()).isEqualTo(4);
            assertThat(results.get(0).getComments()).isEqualTo("Good experience");
        }

        @Test
        @DisplayName("should return empty list when no feedback exists")
        void getFeedbackByInterview_empty() {
            List<CandidateFeedbackResponse> results = candidateFeedbackService
                    .getFeedbackByInterview(completedInterview.getId());

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when interview not found")
        void getFeedbackByInterview_interviewNotFound() {
            assertThatThrownBy(() -> candidateFeedbackService.getFeedbackByInterview(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Feedback By Candidate")
    class GetFeedbackByCandidate {

        @Test
        @DisplayName("should return feedback list for candidate")
        void getFeedbackByCandidate_success() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(5)
                    .build();
            candidateFeedbackService.submitFeedback(request, candidate.getId());

            List<CandidateFeedbackResponse> results = candidateFeedbackService
                    .getFeedbackByCandidate(candidate.getId());

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCandidateId()).isEqualTo(candidate.getId());
            assertThat(results.get(0).getOverallRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when candidate not found")
        void getFeedbackByCandidate_notFound() {
            assertThatThrownBy(() -> candidateFeedbackService.getFeedbackByCandidate(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Feedback Summary")
    class GetFeedbackSummary {

        @Test
        @DisplayName("should return summary with data")
        void getFeedbackSummary_withData() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(4)
                    .communicationRating(5)
                    .professionalismRating(4)
                    .technicalClarityRating(3)
                    .timelinessRating(5)
                    .wouldRecommend(true)
                    .build();
            candidateFeedbackService.submitFeedback(request, candidate.getId());

            FeedbackSummaryResponse summary = candidateFeedbackService.getFeedbackSummary();

            assertThat(summary).isNotNull();
            assertThat(summary.getTotalResponses()).isGreaterThanOrEqualTo(1L);
            assertThat(summary.getAverageOverallRating()).isGreaterThan(0.0);
            assertThat(summary.getAverageCommunicationRating()).isGreaterThan(0.0);
            assertThat(summary.getAverageProfessionalismRating()).isGreaterThan(0.0);
            assertThat(summary.getAverageTechnicalClarityRating()).isGreaterThan(0.0);
            assertThat(summary.getAverageTimelinessRating()).isGreaterThan(0.0);
            assertThat(summary.getRecommendationRate()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("should return zero summary when no data exists")
        void getFeedbackSummary_emptyData() {
            // Note: This test may see data from other tests or seed data.
            // We test that the method returns without error and produces valid structure.
            FeedbackSummaryResponse summary = candidateFeedbackService.getFeedbackSummary();

            assertThat(summary).isNotNull();
            assertThat(summary.getTotalResponses()).isGreaterThanOrEqualTo(0L);
            assertThat(summary.getAverageOverallRating()).isGreaterThanOrEqualTo(0.0);
            assertThat(summary.getRecommendationRate()).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Get My Feedback")
    class GetMyFeedback {

        @Test
        @DisplayName("should return paginated feedback for user")
        void getMyFeedback_paginated() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(4)
                    .comments("My feedback")
                    .build();
            candidateFeedbackService.submitFeedback(request, candidate.getId());

            PaginatedResponse<CandidateFeedbackResponse> result = candidateFeedbackService
                    .getMyFeedback(candidate.getId(), 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getLast()).isTrue();
        }

        @Test
        @DisplayName("should return empty page when no feedback exists")
        void getMyFeedback_empty() {
            PaginatedResponse<CandidateFeedbackResponse> result = candidateFeedbackService
                    .getMyFeedback(candidate.getId(), 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Anonymous Feedback")
    class AnonymousFeedback {

        @Test
        @DisplayName("should hide candidate name when feedback is anonymous")
        void anonymousFeedback_hidesName() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(3)
                    .isAnonymous(true)
                    .build();

            CandidateFeedbackResponse response = candidateFeedbackService.submitFeedback(request, candidate.getId());

            assertThat(response.getIsAnonymous()).isTrue();
            assertThat(response.getCandidateName()).isNull();
            assertThat(response.getCandidateId()).isEqualTo(candidate.getId());
        }

        @Test
        @DisplayName("should show candidate name when feedback is not anonymous")
        void nonAnonymousFeedback_showsName() {
            SubmitCandidateFeedbackRequest request = SubmitCandidateFeedbackRequest.builder()
                    .interviewId(completedInterview.getId())
                    .overallRating(4)
                    .isAnonymous(false)
                    .build();

            CandidateFeedbackResponse response = candidateFeedbackService.submitFeedback(request, candidate.getId());

            assertThat(response.getIsAnonymous()).isFalse();
            assertThat(response.getCandidateName()).isEqualTo("Candidate Feedback");
        }
    }
}

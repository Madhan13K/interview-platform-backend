package com.interview_platform_backend.interview_platform_backend.scorecard.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.candidate.dto.CreateInterviewRequest;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.*;
import com.interview_platform_backend.interview_platform_backend.candidate.service.InterviewService;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.scorecard.dto.*;
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
class EvaluationScorecardServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EvaluationScorecardService scorecardService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserRepository userRepository;

    private User candidate;
    private User interviewer;
    private User scheduler;
    private InterviewResponse completedInterview;

    @BeforeEach
    void setUp() {
        candidate = userRepository.save(User.builder()
                .firstName("Candidate")
                .lastName("Scorecard")
                .email("sc-cand-" + UUID.randomUUID() + "@test.com")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        interviewer = userRepository.save(User.builder()
                .firstName("Interviewer")
                .lastName("Scorecard")
                .email("sc-int-" + UUID.randomUUID() + "@test.com")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        scheduler = userRepository.save(User.builder()
                .firstName("Scheduler")
                .lastName("Scorecard")
                .email("sc-sch-" + UUID.randomUUID() + "@test.com")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        // Create and complete an interview
        CreateInterviewRequest req = CreateInterviewRequest.builder()
                .title("Scorecard Test Interview")
                .candidateId(candidate.getId())
                .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .type(InterviewType.TECHNICAL)
                .mode(InterviewMode.ONLINE)
                .interviewerIds(List.of(interviewer.getId()))
                .build();

        completedInterview = interviewService.createInterview(req, scheduler.getId());
        interviewService.updateStatus(completedInterview.getId(), InterviewStatus.COMPLETED);
    }

    @Nested
    @DisplayName("Criteria Management")
    class CriteriaManagement {

        @Test
        @DisplayName("should create evaluation criteria")
        void createCriteria_success() {
            CreateCriteriaRequest request = CreateCriteriaRequest.builder()
                    .name("Criteria_" + UUID.randomUUID().toString().substring(0, 6))
                    .description("Test criteria")
                    .maxScore(5)
                    .weight(1.5)
                    .orderIndex(1)
                    .build();

            CriteriaResponse response = scorecardService.createCriteria(request, scheduler.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getMaxScore()).isEqualTo(5);
            assertThat(response.getWeight()).isEqualTo(1.5);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate criteria name")
        void createCriteria_duplicate() {
            String name = "Unique_" + UUID.randomUUID().toString().substring(0, 6);
            CreateCriteriaRequest request = CreateCriteriaRequest.builder()
                    .name(name)
                    .build();

            scorecardService.createCriteria(request, null);

            assertThatThrownBy(() -> scorecardService.createCriteria(request, null))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should get all active criteria")
        void getAllCriteria() {
            List<CriteriaResponse> criteria = scorecardService.getAllCriteria();
            // Seeded data should exist
            assertThat(criteria).isNotEmpty();
        }

        @Test
        @DisplayName("should get criteria by interview type")
        void getCriteriaByType() {
            CreateCriteriaRequest request = CreateCriteriaRequest.builder()
                    .name("TypeSpecific_" + UUID.randomUUID().toString().substring(0, 6))
                    .interviewType(InterviewType.TECHNICAL)
                    .build();
            scorecardService.createCriteria(request, null);

            List<CriteriaResponse> criteria = scorecardService.getCriteriaByType(InterviewType.TECHNICAL);
            assertThat(criteria).isNotEmpty();
        }

        @Test
        @DisplayName("should update criteria")
        void updateCriteria() {
            String name = "ToUpdate_" + UUID.randomUUID().toString().substring(0, 6);
            CriteriaResponse created = scorecardService.createCriteria(
                    CreateCriteriaRequest.builder().name(name).build(), null);

            CreateCriteriaRequest updateReq = CreateCriteriaRequest.builder()
                    .description("Updated desc")
                    .maxScore(10)
                    .weight(2.0)
                    .build();

            CriteriaResponse updated = scorecardService.updateCriteria(created.getId(), updateReq);
            assertThat(updated.getDescription()).isEqualTo("Updated desc");
            assertThat(updated.getMaxScore()).isEqualTo(10);
            assertThat(updated.getWeight()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("should soft-delete criteria")
        void deleteCriteria() {
            String name = "ToDelete_" + UUID.randomUUID().toString().substring(0, 6);
            CriteriaResponse created = scorecardService.createCriteria(
                    CreateCriteriaRequest.builder().name(name).build(), null);

            scorecardService.deleteCriteria(created.getId());

            CriteriaResponse found = scorecardService.getCriteriaById(created.getId());
            assertThat(found.getIsActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Scorecard Submission")
    class ScorecardSubmission {

        @Test
        @DisplayName("should submit scorecard successfully")
        void submitScorecard_success() {
            List<CriteriaResponse> criteria = scorecardService.getAllCriteria();
            assertThat(criteria).isNotEmpty();

            List<ScoreEntryRequest> entries = criteria.stream()
                    .limit(3)
                    .map(c -> ScoreEntryRequest.builder()
                            .criteriaId(c.getId())
                            .score(4)
                            .comments("Good performance")
                            .build())
                    .toList();

            SubmitScorecardRequest request = SubmitScorecardRequest.builder()
                    .interviewId(completedInterview.getId())
                    .recommendation(FeedbackRecommendation.HIRE)
                    .overallComments("Strong candidate")
                    .strengths("Problem solving, communication")
                    .weaknesses("Could improve system design")
                    .entries(entries)
                    .build();

            ScorecardResponse response = scorecardService.submitScorecard(request, interviewer.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getOverallScore()).isGreaterThan(0);
            assertThat(response.getRecommendation()).isEqualTo(FeedbackRecommendation.HIRE);
            assertThat(response.getEntries()).hasSize(3);
            assertThat(response.getInterviewerId()).isEqualTo(interviewer.getId());
            assertThat(response.getCandidateId()).isEqualTo(candidate.getId());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate scorecard")
        void submitScorecard_duplicate() {
            List<CriteriaResponse> criteria = scorecardService.getAllCriteria();
            List<ScoreEntryRequest> entries = List.of(ScoreEntryRequest.builder()
                    .criteriaId(criteria.get(0).getId())
                    .score(3)
                    .build());

            SubmitScorecardRequest request = SubmitScorecardRequest.builder()
                    .interviewId(completedInterview.getId())
                    .recommendation(FeedbackRecommendation.HIRE)
                    .entries(entries)
                    .build();

            scorecardService.submitScorecard(request, interviewer.getId());

            assertThatThrownBy(() -> scorecardService.submitScorecard(request, interviewer.getId()))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException for scheduled interview")
        void submitScorecard_invalidStatus() {
            // Create a new SCHEDULED interview
            InterviewResponse scheduled = interviewService.createInterview(
                    CreateInterviewRequest.builder()
                            .title("Scheduled Only")
                            .candidateId(candidate.getId())
                            .startTime(Instant.now().plus(2, ChronoUnit.DAYS))
                            .endTime(Instant.now().plus(2, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                            .type(InterviewType.HR)
                            .mode(InterviewMode.ONLINE)
                            .interviewerIds(List.of(interviewer.getId()))
                            .build(), scheduler.getId());

            List<CriteriaResponse> criteria = scorecardService.getAllCriteria();
            SubmitScorecardRequest request = SubmitScorecardRequest.builder()
                    .interviewId(scheduled.getId())
                    .recommendation(FeedbackRecommendation.HOLD)
                    .entries(List.of(ScoreEntryRequest.builder()
                            .criteriaId(criteria.get(0).getId())
                            .score(3)
                            .build()))
                    .build();

            assertThatThrownBy(() -> scorecardService.submitScorecard(request, interviewer.getId()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException when score exceeds max")
        void submitScorecard_exceedsMaxScore() {
            List<CriteriaResponse> criteria = scorecardService.getAllCriteria();
            SubmitScorecardRequest request = SubmitScorecardRequest.builder()
                    .interviewId(completedInterview.getId())
                    .recommendation(FeedbackRecommendation.HIRE)
                    .entries(List.of(ScoreEntryRequest.builder()
                            .criteriaId(criteria.get(0).getId())
                            .score(99) // exceeds max of 5
                            .build()))
                    .build();

            assertThatThrownBy(() -> scorecardService.submitScorecard(request, interviewer.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("exceeds max score");
        }
    }

    @Nested
    @DisplayName("Scorecard Retrieval")
    class ScorecardRetrieval {

        private ScorecardResponse submitTestScorecard() {
            List<CriteriaResponse> criteria = scorecardService.getAllCriteria();
            List<ScoreEntryRequest> entries = criteria.stream().limit(2)
                    .map(c -> ScoreEntryRequest.builder()
                            .criteriaId(c.getId()).score(4).build())
                    .toList();

            return scorecardService.submitScorecard(SubmitScorecardRequest.builder()
                    .interviewId(completedInterview.getId())
                    .recommendation(FeedbackRecommendation.HIRE)
                    .entries(entries)
                    .build(), interviewer.getId());
        }

        @Test
        @DisplayName("should get scorecard by ID")
        void getScorecard() {
            ScorecardResponse submitted = submitTestScorecard();
            ScorecardResponse found = scorecardService.getScorecard(submitted.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(submitted.getId());
        }

        @Test
        @DisplayName("should get scorecards by interview")
        void getScorecardsByInterview() {
            submitTestScorecard();
            List<ScorecardResponse> scorecards = scorecardService.getScorecardsByInterview(completedInterview.getId());

            assertThat(scorecards).hasSize(1);
        }

        @Test
        @DisplayName("should get scorecard by interview and interviewer")
        void getScorecardByInterviewAndInterviewer() {
            submitTestScorecard();
            ScorecardResponse found = scorecardService.getScorecardByInterviewAndInterviewer(
                    completedInterview.getId(), interviewer.getId());

            assertThat(found).isNotNull();
            assertThat(found.getInterviewerId()).isEqualTo(interviewer.getId());
        }

        @Test
        @DisplayName("should get scorecards by candidate")
        void getScorecardsByCandidate() {
            submitTestScorecard();
            List<ScorecardResponse> scorecards = scorecardService.getScorecardsByCandidate(candidate.getId());

            assertThat(scorecards).isNotEmpty();
            assertThat(scorecards).allMatch(s -> s.getCandidateId().equals(candidate.getId()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid scorecard ID")
        void getScorecard_notFound() {
            assertThatThrownBy(() -> scorecardService.getScorecard(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Candidate Summary")
    class Summary {

        @Test
        @DisplayName("should return candidate summary with scores")
        void getCandidateSummary() {
            // Submit a scorecard
            List<CriteriaResponse> criteria = scorecardService.getAllCriteria();
            List<ScoreEntryRequest> entries = criteria.stream().limit(3)
                    .map(c -> ScoreEntryRequest.builder()
                            .criteriaId(c.getId()).score(4).build())
                    .toList();

            scorecardService.submitScorecard(SubmitScorecardRequest.builder()
                    .interviewId(completedInterview.getId())
                    .recommendation(FeedbackRecommendation.HIRE)
                    .entries(entries)
                    .build(), interviewer.getId());

            CandidateScorecardSummary summary = scorecardService.getCandidateSummary(completedInterview.getId());

            assertThat(summary).isNotNull();
            assertThat(summary.getCandidateId()).isEqualTo(candidate.getId());
            assertThat(summary.getTotalScorecards()).isEqualTo(1);
            assertThat(summary.getAverageOverallScore()).isGreaterThan(0);
            assertThat(summary.getAverageScoreByCriteria()).isNotEmpty();
            assertThat(summary.getRecommendationBreakdown()).containsKey(FeedbackRecommendation.HIRE);
        }

        @Test
        @DisplayName("should return empty summary when no scorecards")
        void getCandidateSummary_empty() {
            CandidateScorecardSummary summary = scorecardService.getCandidateSummary(completedInterview.getId());

            assertThat(summary.getTotalScorecards()).isEqualTo(0);
            assertThat(summary.getAverageOverallScore()).isEqualTo(0.0);
        }
    }
}


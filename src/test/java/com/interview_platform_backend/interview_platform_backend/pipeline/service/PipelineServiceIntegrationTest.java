package com.interview_platform_backend.interview_platform_backend.pipeline.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.pipeline.dto.*;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.CandidatePipelineStatus;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.StageStatus;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class PipelineServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private UserRepository userRepository;

    private User recruiter;
    private User candidate;
    private String pipelineName;

    @BeforeEach
    void setUp() {
        pipelineName = "Pipeline_" + UUID.randomUUID().toString().substring(0, 8);

        recruiter = userRepository.save(User.builder()
                .firstName("Recruiter")
                .lastName("Pipeline")
                .email("recruiter-pl-" + UUID.randomUUID() + "@test.com")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        candidate = userRepository.save(User.builder()
                .firstName("Candidate")
                .lastName("Pipeline")
                .email("candidate-pl-" + UUID.randomUUID() + "@test.com")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
    }

    private CreatePipelineRequest buildCreateRequest() {
        return CreatePipelineRequest.builder()
                .name(pipelineName)
                .description("Standard hiring pipeline")
                .department("Engineering")
                .stages(List.of(
                        PipelineStageRequest.builder()
                                .name("Phone Screening")
                                .orderIndex(1)
                                .interviewType(InterviewType.SCREENING)
                                .durationMinutes(30)
                                .isOptional(false)
                                .build(),
                        PipelineStageRequest.builder()
                                .name("Technical Round")
                                .orderIndex(2)
                                .interviewType(InterviewType.TECHNICAL)
                                .durationMinutes(60)
                                .isOptional(false)
                                .build(),
                        PipelineStageRequest.builder()
                                .name("HR Round")
                                .orderIndex(3)
                                .interviewType(InterviewType.HR)
                                .durationMinutes(45)
                                .isOptional(false)
                                .build(),
                        PipelineStageRequest.builder()
                                .name("Final Round")
                                .orderIndex(4)
                                .interviewType(InterviewType.FINAL)
                                .durationMinutes(60)
                                .isOptional(false)
                                .build()
                ))
                .build();
    }

    @Nested
    @DisplayName("Pipeline CRUD")
    class PipelineCRUD {

        @Test
        @DisplayName("should create pipeline with stages")
        void createPipeline_success() {
            PipelineResponse response = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getName()).isEqualTo(pipelineName);
            assertThat(response.getDepartment()).isEqualTo("Engineering");
            assertThat(response.getStages()).hasSize(4);
            assertThat(response.getStages().get(0).getName()).isEqualTo("Phone Screening");
            assertThat(response.getStages().get(3).getName()).isEqualTo("Final Round");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate name")
        void createPipeline_duplicate() {
            pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());
            assertThatThrownBy(() -> pipelineService.createPipeline(buildCreateRequest(), recruiter.getId()))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should get pipeline by ID")
        void getPipeline() {
            PipelineResponse created = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());
            PipelineResponse found = pipelineService.getPipeline(created.getId());

            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getStages()).hasSize(4);
        }

        @Test
        @DisplayName("should get all active pipelines")
        void getAllPipelines() {
            pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());
            List<PipelineResponse> pipelines = pipelineService.getAllPipelines();

            assertThat(pipelines).isNotEmpty();
        }

        @Test
        @DisplayName("should update pipeline")
        void updatePipeline() {
            PipelineResponse created = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());

            UpdatePipelineRequest update = UpdatePipelineRequest.builder()
                    .description("Updated description")
                    .department("Product")
                    .build();

            PipelineResponse updated = pipelineService.updatePipeline(created.getId(), update);
            assertThat(updated.getDescription()).isEqualTo("Updated description");
            assertThat(updated.getDepartment()).isEqualTo("Product");
        }

        @Test
        @DisplayName("should delete pipeline")
        void deletePipeline() {
            PipelineResponse created = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());
            pipelineService.deletePipeline(created.getId());

            assertThatThrownBy(() -> pipelineService.getPipeline(created.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Candidate Pipeline Management")
    class CandidateManagement {

        @Test
        @DisplayName("should add candidate to pipeline")
        void addCandidate_success() {
            PipelineResponse pipeline = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());

            AddCandidateToPipelineRequest request = AddCandidateToPipelineRequest.builder()
                    .pipelineId(pipeline.getId())
                    .candidateId(candidate.getId())
                    .notes("Strong resume")
                    .build();

            CandidatePipelineResponse response = pipelineService.addCandidateToPipeline(request);

            assertThat(response).isNotNull();
            assertThat(response.getCandidateId()).isEqualTo(candidate.getId());
            assertThat(response.getStatus()).isEqualTo(CandidatePipelineStatus.ACTIVE);
            assertThat(response.getCurrentStageName()).isEqualTo("Phone Screening");
            assertThat(response.getCurrentStageOrder()).isEqualTo(1);
            assertThat(response.getTotalStages()).isEqualTo(4);
            assertThat(response.getStageProgress()).hasSize(4);
            // First stage should be IN_PROGRESS
            assertThat(response.getStageProgress().get(0).getStatus()).isEqualTo(StageStatus.IN_PROGRESS);
            // Rest should be PENDING
            assertThat(response.getStageProgress().get(1).getStatus()).isEqualTo(StageStatus.PENDING);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when candidate already in pipeline")
        void addCandidate_duplicate() {
            PipelineResponse pipeline = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());

            AddCandidateToPipelineRequest request = AddCandidateToPipelineRequest.builder()
                    .pipelineId(pipeline.getId())
                    .candidateId(candidate.getId())
                    .build();

            pipelineService.addCandidateToPipeline(request);
            assertThatThrownBy(() -> pipelineService.addCandidateToPipeline(request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should get candidates in pipeline")
        void getCandidatesInPipeline() {
            PipelineResponse pipeline = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());
            pipelineService.addCandidateToPipeline(AddCandidateToPipelineRequest.builder()
                    .pipelineId(pipeline.getId())
                    .candidateId(candidate.getId())
                    .build());

            List<CandidatePipelineResponse> candidates = pipelineService.getCandidatesInPipeline(pipeline.getId());
            assertThat(candidates).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Stage Progression")
    class StageProgression {

        private CandidatePipelineResponse setupCandidateInPipeline() {
            PipelineResponse pipeline = pipelineService.createPipeline(buildCreateRequest(), recruiter.getId());
            return pipelineService.addCandidateToPipeline(AddCandidateToPipelineRequest.builder()
                    .pipelineId(pipeline.getId())
                    .candidateId(candidate.getId())
                    .build());
        }

        @Test
        @DisplayName("should advance candidate to next stage")
        void advanceStage_success() {
            CandidatePipelineResponse cp = setupCandidateInPipeline();

            CandidatePipelineResponse advanced = pipelineService.advanceToNextStage(
                    cp.getId(), "Passed phone screening");

            assertThat(advanced.getCurrentStageName()).isEqualTo("Technical Round");
            assertThat(advanced.getCurrentStageOrder()).isEqualTo(2);
            assertThat(advanced.getStatus()).isEqualTo(CandidatePipelineStatus.ACTIVE);

            // First stage should be COMPLETED
            CandidatePipelineResponse.StageProgressResponse first = advanced.getStageProgress().stream()
                    .filter(s -> s.getStageOrder() == 1).findFirst().orElseThrow();
            assertThat(first.getStatus()).isEqualTo(StageStatus.COMPLETED);
            assertThat(first.getFeedback()).isEqualTo("Passed phone screening");

            // Second stage should be IN_PROGRESS
            CandidatePipelineResponse.StageProgressResponse second = advanced.getStageProgress().stream()
                    .filter(s -> s.getStageOrder() == 2).findFirst().orElseThrow();
            assertThat(second.getStatus()).isEqualTo(StageStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should advance through all stages to HIRED")
        void advanceThroughAll_hired() {
            CandidatePipelineResponse cp = setupCandidateInPipeline();

            // Advance through all 4 stages
            cp = pipelineService.advanceToNextStage(cp.getId(), "Stage 1 pass");
            cp = pipelineService.advanceToNextStage(cp.getId(), "Stage 2 pass");
            cp = pipelineService.advanceToNextStage(cp.getId(), "Stage 3 pass");
            cp = pipelineService.advanceToNextStage(cp.getId(), "Final pass");

            assertThat(cp.getStatus()).isEqualTo(CandidatePipelineStatus.HIRED);
            assertThat(cp.getCurrentStageName()).isNull();
        }

        @Test
        @DisplayName("should reject candidate")
        void rejectCandidate() {
            CandidatePipelineResponse cp = setupCandidateInPipeline();

            CandidatePipelineResponse rejected = pipelineService.rejectCandidate(
                    cp.getId(), "Not enough experience");

            assertThat(rejected.getStatus()).isEqualTo(CandidatePipelineStatus.REJECTED);
            // Current stage should be REJECTED
            CandidatePipelineResponse.StageProgressResponse current = rejected.getStageProgress().stream()
                    .filter(s -> s.getStageOrder() == 1).findFirst().orElseThrow();
            assertThat(current.getStatus()).isEqualTo(StageStatus.REJECTED);
        }

        @Test
        @DisplayName("should not advance rejected candidate")
        void advanceRejected_fails() {
            CandidatePipelineResponse cp = setupCandidateInPipeline();
            pipelineService.rejectCandidate(cp.getId(), "Rejected");

            assertThatThrownBy(() -> pipelineService.advanceToNextStage(cp.getId(), "try"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should update stage progress with interview link")
        void updateStageProgress() {
            CandidatePipelineResponse cp = setupCandidateInPipeline();
            UUID stageId = cp.getStageProgress().get(0).getStageId();

            UpdateStageProgressRequest request = UpdateStageProgressRequest.builder()
                    .feedback("Detailed feedback")
                    .build();

            CandidatePipelineResponse updated = pipelineService.updateStageProgress(
                    cp.getId(), stageId, request);

            CandidatePipelineResponse.StageProgressResponse stage = updated.getStageProgress().stream()
                    .filter(s -> s.getStageId().equals(stageId)).findFirst().orElseThrow();
            assertThat(stage.getFeedback()).isEqualTo("Detailed feedback");
        }

        @Test
        @DisplayName("should update candidate pipeline status")
        void updateStatus() {
            CandidatePipelineResponse cp = setupCandidateInPipeline();

            CandidatePipelineResponse updated = pipelineService.updateCandidatePipelineStatus(
                    cp.getId(), CandidatePipelineStatus.ON_HOLD);

            assertThat(updated.getStatus()).isEqualTo(CandidatePipelineStatus.ON_HOLD);
        }
    }
}


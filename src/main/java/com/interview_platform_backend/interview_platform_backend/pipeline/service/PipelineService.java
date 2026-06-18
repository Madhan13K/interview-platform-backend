package com.interview_platform_backend.interview_platform_backend.pipeline.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.pipeline.dto.*;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.*;
import com.interview_platform_backend.interview_platform_backend.pipeline.repository.*;
import com.interview_platform_backend.interview_platform_backend.template.entity.InterviewTemplate;
import com.interview_platform_backend.interview_platform_backend.template.repository.InterviewTemplateRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PipelineService {

    private final InterviewPipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;
    private final CandidatePipelineRepository candidatePipelineRepository;
    private final CandidateStageProgressRepository progressRepository;
    private final InterviewTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;

    public PipelineService(InterviewPipelineRepository pipelineRepository,
                           PipelineStageRepository stageRepository,
                           CandidatePipelineRepository candidatePipelineRepository,
                           CandidateStageProgressRepository progressRepository,
                           InterviewTemplateRepository templateRepository,
                           UserRepository userRepository,
                           InterviewRepository interviewRepository) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.candidatePipelineRepository = candidatePipelineRepository;
        this.progressRepository = progressRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.interviewRepository = interviewRepository;
    }

    // ==================== Pipeline CRUD ====================

    @CacheEvict(value = "pipelines", allEntries = true)
    public PipelineResponse createPipeline(CreatePipelineRequest request, UUID createdByUserId) {
        if (pipelineRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Pipeline", "name", request.getName());
        }

        User createdBy = createdByUserId != null ?
                userRepository.findById(createdByUserId).orElse(null) : null;

        InterviewPipeline pipeline = InterviewPipeline.builder()
                .name(request.getName())
                .description(request.getDescription())
                .department(request.getDepartment())
                .createdBy(createdBy)
                .stages(new ArrayList<>())
                .build();

        InterviewPipeline saved = pipelineRepository.save(pipeline);

        for (PipelineStageRequest stageReq : request.getStages()) {
            PipelineStage stage = buildStage(saved, stageReq);
            saved.getStages().add(stage);
        }

        saved = pipelineRepository.save(saved);
        return toPipelineResponse(saved);
    }

    @Cacheable(value = "pipelines", key = "#pipelineId")
    @Transactional(readOnly = true)
    public PipelineResponse getPipeline(UUID pipelineId) {
        InterviewPipeline pipeline = pipelineRepository.findByIdWithStages(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline", "id", pipelineId));
        return toPipelineResponse(pipeline);
    }

    @Transactional(readOnly = true)
    public List<PipelineResponse> getAllPipelines() {
        return pipelineRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toPipelineResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PipelineResponse> getPipelinesByDepartment(String department) {
        return pipelineRepository.findByDepartmentAndIsActiveTrue(department).stream()
                .map(this::toPipelineResponse)
                .toList();
    }

    @CacheEvict(value = "pipelines", allEntries = true)
    public PipelineResponse updatePipeline(UUID pipelineId, UpdatePipelineRequest request) {
        InterviewPipeline pipeline = pipelineRepository.findByIdWithStages(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline", "id", pipelineId));

        if (request.getName() != null) {
            if (!request.getName().equals(pipeline.getName()) && pipelineRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Pipeline", "name", request.getName());
            }
            pipeline.setName(request.getName());
        }
        if (request.getDescription() != null) pipeline.setDescription(request.getDescription());
        if (request.getDepartment() != null) pipeline.setDepartment(request.getDepartment());
        if (request.getIsActive() != null) pipeline.setIsActive(request.getIsActive());

        if (request.getStages() != null) {
            pipeline.getStages().clear();
            for (PipelineStageRequest stageReq : request.getStages()) {
                pipeline.getStages().add(buildStage(pipeline, stageReq));
            }
        }

        return toPipelineResponse(pipelineRepository.save(pipeline));
    }

    @CacheEvict(value = "pipelines", allEntries = true)
    public void deletePipeline(UUID pipelineId) {
        InterviewPipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline", "id", pipelineId));
        pipelineRepository.delete(pipeline);
    }

    // ==================== Candidate Pipeline Management ====================

    public CandidatePipelineResponse addCandidateToPipeline(AddCandidateToPipelineRequest request) {
        InterviewPipeline pipeline = pipelineRepository.findByIdWithStages(request.getPipelineId())
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline", "id", request.getPipelineId()));

        User candidate = userRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getCandidateId()));

        if (candidatePipelineRepository.existsByPipelineIdAndCandidateId(
                request.getPipelineId(), request.getCandidateId())) {
            throw new DuplicateResourceException("Candidate is already in this pipeline");
        }

        if (pipeline.getStages().isEmpty()) {
            throw new BadRequestException("Pipeline has no stages defined");
        }

        PipelineStage firstStage = pipeline.getStages().get(0);

        CandidatePipeline cp = CandidatePipeline.builder()
                .pipeline(pipeline)
                .candidate(candidate)
                .currentStage(firstStage)
                .status(CandidatePipelineStatus.ACTIVE)
                .notes(request.getNotes())
                .stageProgress(new ArrayList<>())
                .build();

        // Create progress entries for all stages
        for (PipelineStage stage : pipeline.getStages()) {
            CandidateStageProgress progress = CandidateStageProgress.builder()
                    .candidatePipeline(cp)
                    .stage(stage)
                    .status(stage.equals(firstStage) ? StageStatus.IN_PROGRESS : StageStatus.PENDING)
                    .startedAt(stage.equals(firstStage) ? Instant.now() : null)
                    .build();
            cp.getStageProgress().add(progress);
        }

        CandidatePipeline saved = candidatePipelineRepository.save(cp);
        return toCandidatePipelineResponse(saved, pipeline.getStages().size());
    }

    @Transactional(readOnly = true)
    public CandidatePipelineResponse getCandidatePipeline(UUID candidatePipelineId) {
        CandidatePipeline cp = candidatePipelineRepository.findByIdWithDetails(candidatePipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidatePipeline", "id", candidatePipelineId));
        int totalStages = cp.getPipeline().getStages() != null ? cp.getPipeline().getStages().size() :
                stageRepository.findByPipelineIdOrderByOrderIndex(cp.getPipeline().getId()).size();
        return toCandidatePipelineResponse(cp, totalStages);
    }

    @Transactional(readOnly = true)
    public List<CandidatePipelineResponse> getCandidatesInPipeline(UUID pipelineId) {
        InterviewPipeline pipeline = pipelineRepository.findByIdWithStages(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline", "id", pipelineId));

        return candidatePipelineRepository.findByPipelineId(pipelineId).stream()
                .map(cp -> toCandidatePipelineResponse(cp, pipeline.getStages().size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidatePipelineResponse> getCandidatePipelines(UUID candidateId) {
        return candidatePipelineRepository.findByCandidateId(candidateId).stream()
                .map(cp -> {
                    int total = cp.getPipeline().getStages() != null ? cp.getPipeline().getStages().size() : 0;
                    return toCandidatePipelineResponse(cp, total);
                })
                .toList();
    }

    // ==================== Stage Progression ====================

    public CandidatePipelineResponse advanceToNextStage(UUID candidatePipelineId, String feedback) {
        CandidatePipeline cp = candidatePipelineRepository.findByIdWithDetails(candidatePipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidatePipeline", "id", candidatePipelineId));

        if (cp.getStatus() != CandidatePipelineStatus.ACTIVE) {
            throw new BadRequestException("Cannot advance a candidate with status: " + cp.getStatus());
        }

        // Mark current stage as completed
        CandidateStageProgress currentProgress = cp.getStageProgress().stream()
                .filter(sp -> sp.getStage().getId().equals(cp.getCurrentStage().getId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Current stage progress not found"));

        currentProgress.setStatus(StageStatus.COMPLETED);
        currentProgress.setCompletedAt(Instant.now());
        if (feedback != null) currentProgress.setFeedback(feedback);

        // Find next stage
        List<PipelineStage> stages = stageRepository.findByPipelineIdOrderByOrderIndex(cp.getPipeline().getId());
        int currentIndex = -1;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).getId().equals(cp.getCurrentStage().getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex >= stages.size() - 1) {
            // Last stage — mark pipeline as completed (hired)
            cp.setCurrentStage(null);
            cp.setStatus(CandidatePipelineStatus.HIRED);
            cp.setCompletedAt(Instant.now());
        } else {
            PipelineStage nextStage = stages.get(currentIndex + 1);
            cp.setCurrentStage(nextStage);

            CandidateStageProgress nextProgress = cp.getStageProgress().stream()
                    .filter(sp -> sp.getStage().getId().equals(nextStage.getId()))
                    .findFirst()
                    .orElse(null);

            if (nextProgress != null) {
                nextProgress.setStatus(StageStatus.IN_PROGRESS);
                nextProgress.setStartedAt(Instant.now());
            }
        }

        CandidatePipeline saved = candidatePipelineRepository.save(cp);
        return toCandidatePipelineResponse(saved, stages.size());
    }

    public CandidatePipelineResponse rejectCandidate(UUID candidatePipelineId, String feedback) {
        CandidatePipeline cp = candidatePipelineRepository.findByIdWithDetails(candidatePipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidatePipeline", "id", candidatePipelineId));

        if (cp.getStatus() != CandidatePipelineStatus.ACTIVE) {
            throw new BadRequestException("Cannot reject a candidate with status: " + cp.getStatus());
        }

        // Mark current stage as rejected
        if (cp.getCurrentStage() != null) {
            cp.getStageProgress().stream()
                    .filter(sp -> sp.getStage().getId().equals(cp.getCurrentStage().getId()))
                    .findFirst()
                    .ifPresent(progress -> {
                        progress.setStatus(StageStatus.REJECTED);
                        progress.setCompletedAt(Instant.now());
                        if (feedback != null) progress.setFeedback(feedback);
                    });
        }

        cp.setStatus(CandidatePipelineStatus.REJECTED);
        cp.setCompletedAt(Instant.now());

        int totalStages = stageRepository.findByPipelineIdOrderByOrderIndex(cp.getPipeline().getId()).size();
        return toCandidatePipelineResponse(candidatePipelineRepository.save(cp), totalStages);
    }

    public CandidatePipelineResponse updateStageProgress(UUID candidatePipelineId, UUID stageId,
                                                          UpdateStageProgressRequest request) {
        CandidatePipeline cp = candidatePipelineRepository.findByIdWithDetails(candidatePipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidatePipeline", "id", candidatePipelineId));

        CandidateStageProgress progress = cp.getStageProgress().stream()
                .filter(sp -> sp.getStage().getId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Stage progress not found for stage: " + stageId));

        if (request.getStatus() != null) progress.setStatus(request.getStatus());
        if (request.getFeedback() != null) progress.setFeedback(request.getFeedback());
        if (request.getInterviewId() != null) {
            Interview interview = interviewRepository.findById(request.getInterviewId())
                    .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", request.getInterviewId()));
            progress.setInterview(interview);
        }

        int totalStages = stageRepository.findByPipelineIdOrderByOrderIndex(cp.getPipeline().getId()).size();
        return toCandidatePipelineResponse(candidatePipelineRepository.save(cp), totalStages);
    }

    public CandidatePipelineResponse updateCandidatePipelineStatus(UUID candidatePipelineId,
                                                                     CandidatePipelineStatus status) {
        CandidatePipeline cp = candidatePipelineRepository.findByIdWithDetails(candidatePipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidatePipeline", "id", candidatePipelineId));

        cp.setStatus(status);
        if (status == CandidatePipelineStatus.HIRED || status == CandidatePipelineStatus.REJECTED
                || status == CandidatePipelineStatus.WITHDRAWN) {
            cp.setCompletedAt(Instant.now());
        }

        int totalStages = stageRepository.findByPipelineIdOrderByOrderIndex(cp.getPipeline().getId()).size();
        return toCandidatePipelineResponse(candidatePipelineRepository.save(cp), totalStages);
    }

    // ==================== Helpers ====================

    private PipelineStage buildStage(InterviewPipeline pipeline, PipelineStageRequest req) {
        InterviewTemplate template = null;
        if (req.getTemplateId() != null) {
            template = templateRepository.findById(req.getTemplateId()).orElse(null);
        }

        return PipelineStage.builder()
                .pipeline(pipeline)
                .name(req.getName())
                .description(req.getDescription())
                .orderIndex(req.getOrderIndex())
                .interviewType(req.getInterviewType())
                .template(template)
                .durationMinutes(req.getDurationMinutes())
                .isOptional(req.getIsOptional() != null ? req.getIsOptional() : false)
                .build();
    }

    private PipelineResponse toPipelineResponse(InterviewPipeline p) {
        List<PipelineResponse.PipelineStageResponse> stages = List.of();
        if (p.getStages() != null) {
            stages = p.getStages().stream()
                    .map(s -> PipelineResponse.PipelineStageResponse.builder()
                            .id(s.getId())
                            .name(s.getName())
                            .description(s.getDescription())
                            .orderIndex(s.getOrderIndex())
                            .interviewType(s.getInterviewType())
                            .templateId(s.getTemplate() != null ? s.getTemplate().getId() : null)
                            .templateName(s.getTemplate() != null ? s.getTemplate().getTitle() : null)
                            .durationMinutes(s.getDurationMinutes())
                            .isOptional(s.getIsOptional())
                            .build())
                    .toList();
        }

        return PipelineResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .department(p.getDepartment())
                .isActive(p.getIsActive())
                .createdById(p.getCreatedBy() != null ? p.getCreatedBy().getId() : null)
                .createdByName(p.getCreatedBy() != null ?
                        p.getCreatedBy().getFirstName() + " " + p.getCreatedBy().getLastName() : null)
                .stages(stages)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private CandidatePipelineResponse toCandidatePipelineResponse(CandidatePipeline cp, int totalStages) {
        List<CandidatePipelineResponse.StageProgressResponse> progressList = List.of();
        if (cp.getStageProgress() != null) {
            progressList = cp.getStageProgress().stream()
                    .map(sp -> CandidatePipelineResponse.StageProgressResponse.builder()
                            .id(sp.getId())
                            .stageId(sp.getStage().getId())
                            .stageName(sp.getStage().getName())
                            .stageOrder(sp.getStage().getOrderIndex())
                            .status(sp.getStatus())
                            .interviewId(sp.getInterview() != null ? sp.getInterview().getId() : null)
                            .feedback(sp.getFeedback())
                            .startedAt(sp.getStartedAt())
                            .completedAt(sp.getCompletedAt())
                            .build())
                    .toList();
        }

        return CandidatePipelineResponse.builder()
                .id(cp.getId())
                .pipelineId(cp.getPipeline().getId())
                .pipelineName(cp.getPipeline().getName())
                .candidateId(cp.getCandidate().getId())
                .candidateName(cp.getCandidate().getFirstName() + " " + cp.getCandidate().getLastName())
                .candidateEmail(cp.getCandidate().getEmail())
                .currentStageId(cp.getCurrentStage() != null ? cp.getCurrentStage().getId() : null)
                .currentStageName(cp.getCurrentStage() != null ? cp.getCurrentStage().getName() : null)
                .currentStageOrder(cp.getCurrentStage() != null ? cp.getCurrentStage().getOrderIndex() : null)
                .totalStages(totalStages)
                .status(cp.getStatus())
                .notes(cp.getNotes())
                .stageProgress(progressList)
                .startedAt(cp.getStartedAt())
                .completedAt(cp.getCompletedAt())
                .build();
    }
}


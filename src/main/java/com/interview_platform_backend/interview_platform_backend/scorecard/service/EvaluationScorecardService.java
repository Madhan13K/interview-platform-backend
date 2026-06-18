package com.interview_platform_backend.interview_platform_backend.scorecard.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.FeedbackRecommendation;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.scorecard.dto.*;
import com.interview_platform_backend.interview_platform_backend.scorecard.entity.EvaluationCriteria;
import com.interview_platform_backend.interview_platform_backend.scorecard.entity.EvaluationScorecard;
import com.interview_platform_backend.interview_platform_backend.scorecard.entity.ScorecardEntry;
import com.interview_platform_backend.interview_platform_backend.scorecard.repository.EvaluationCriteriaRepository;
import com.interview_platform_backend.interview_platform_backend.scorecard.repository.EvaluationScorecardRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EvaluationScorecardService {

    private final EvaluationCriteriaRepository criteriaRepository;
    private final EvaluationScorecardRepository scorecardRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public EvaluationScorecardService(EvaluationCriteriaRepository criteriaRepository,
                                       EvaluationScorecardRepository scorecardRepository,
                                       InterviewRepository interviewRepository,
                                       UserRepository userRepository) {
        this.criteriaRepository = criteriaRepository;
        this.scorecardRepository = scorecardRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    // ==================== Criteria Management ====================

    @CacheEvict(value = "evaluationCriteria", allEntries = true)
    public CriteriaResponse createCriteria(CreateCriteriaRequest request, UUID createdByUserId) {
        if (criteriaRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("EvaluationCriteria", "name", request.getName());
        }

        User createdBy = createdByUserId != null ?
                userRepository.findById(createdByUserId).orElse(null) : null;

        EvaluationCriteria criteria = EvaluationCriteria.builder()
                .name(request.getName())
                .description(request.getDescription())
                .interviewType(request.getInterviewType())
                .maxScore(request.getMaxScore() != null ? request.getMaxScore() : 5)
                .weight(request.getWeight() != null ? request.getWeight() : 1.0)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .createdBy(createdBy)
                .build();

        return toCriteriaResponse(criteriaRepository.save(criteria));
    }

    @Transactional(readOnly = true)
    public List<CriteriaResponse> getAllCriteria() {
        return criteriaRepository.findByIsActiveTrueOrderByOrderIndex().stream()
                .map(this::toCriteriaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CriteriaResponse> getCriteriaByType(InterviewType type) {
        List<EvaluationCriteria> typeCriteria = criteriaRepository
                .findByInterviewTypeAndIsActiveTrueOrderByOrderIndex(type);
        List<EvaluationCriteria> globalCriteria = criteriaRepository
                .findByInterviewTypeIsNullAndIsActiveTrueOrderByOrderIndex();

        List<CriteriaResponse> result = new ArrayList<>();
        result.addAll(globalCriteria.stream().map(this::toCriteriaResponse).toList());
        result.addAll(typeCriteria.stream().map(this::toCriteriaResponse).toList());
        return result;
    }

    @Transactional(readOnly = true)
    public CriteriaResponse getCriteriaById(UUID criteriaId) {
        EvaluationCriteria criteria = criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new ResourceNotFoundException("EvaluationCriteria", "id", criteriaId));
        return toCriteriaResponse(criteria);
    }

    @CacheEvict(value = "evaluationCriteria", allEntries = true)
    public CriteriaResponse updateCriteria(UUID criteriaId, CreateCriteriaRequest request) {
        EvaluationCriteria criteria = criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new ResourceNotFoundException("EvaluationCriteria", "id", criteriaId));

        if (request.getName() != null && !request.getName().equals(criteria.getName())
                && criteriaRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("EvaluationCriteria", "name", request.getName());
        }

        if (request.getName() != null) criteria.setName(request.getName());
        if (request.getDescription() != null) criteria.setDescription(request.getDescription());
        if (request.getInterviewType() != null) criteria.setInterviewType(request.getInterviewType());
        if (request.getMaxScore() != null) criteria.setMaxScore(request.getMaxScore());
        if (request.getWeight() != null) criteria.setWeight(request.getWeight());
        if (request.getOrderIndex() != null) criteria.setOrderIndex(request.getOrderIndex());

        return toCriteriaResponse(criteriaRepository.save(criteria));
    }

    @CacheEvict(value = "evaluationCriteria", allEntries = true)
    public void deleteCriteria(UUID criteriaId) {
        EvaluationCriteria criteria = criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new ResourceNotFoundException("EvaluationCriteria", "id", criteriaId));
        criteria.setIsActive(false);
        criteriaRepository.save(criteria);
    }

    // ==================== Scorecard Submission ====================

    public ScorecardResponse submitScorecard(SubmitScorecardRequest request, UUID interviewerId) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", request.getInterviewId()));

        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        if (interview.getStatus() != InterviewStatus.COMPLETED && interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BadRequestException("Scorecards can only be submitted for completed or in-progress interviews");
        }

        if (scorecardRepository.existsByInterviewIdAndInterviewerId(request.getInterviewId(), interviewerId)) {
            throw new DuplicateResourceException("Scorecard already submitted for this interview by this interviewer");
        }

        EvaluationScorecard scorecard = EvaluationScorecard.builder()
                .interview(interview)
                .interviewer(interviewer)
                .recommendation(request.getRecommendation())
                .overallComments(request.getOverallComments())
                .strengths(request.getStrengths())
                .weaknesses(request.getWeaknesses())
                .entries(new ArrayList<>())
                .build();

        for (ScoreEntryRequest entryReq : request.getEntries()) {
            EvaluationCriteria criteria = criteriaRepository.findById(entryReq.getCriteriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("EvaluationCriteria", "id", entryReq.getCriteriaId()));

            if (entryReq.getScore() > criteria.getMaxScore()) {
                throw new BadRequestException("Score for '" + criteria.getName() +
                        "' exceeds max score of " + criteria.getMaxScore());
            }

            ScorecardEntry entry = ScorecardEntry.builder()
                    .scorecard(scorecard)
                    .criteria(criteria)
                    .score(entryReq.getScore())
                    .comments(entryReq.getComments())
                    .build();

            scorecard.getEntries().add(entry);
        }

        scorecard.setOverallScore(scorecard.calculateWeightedScore());
        EvaluationScorecard saved = scorecardRepository.save(scorecard);

        return toScorecardResponse(saved);
    }

    // ==================== Scorecard Retrieval ====================

    @Transactional(readOnly = true)
    public ScorecardResponse getScorecard(UUID scorecardId) {
        EvaluationScorecard scorecard = scorecardRepository.findByIdWithEntries(scorecardId)
                .orElseThrow(() -> new ResourceNotFoundException("EvaluationScorecard", "id", scorecardId));
        return toScorecardResponse(scorecard);
    }

    @Transactional(readOnly = true)
    public List<ScorecardResponse> getScorecardsByInterview(UUID interviewId) {
        return scorecardRepository.findByInterviewIdWithEntries(interviewId).stream()
                .map(this::toScorecardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScorecardResponse getScorecardByInterviewAndInterviewer(UUID interviewId, UUID interviewerId) {
        EvaluationScorecard scorecard = scorecardRepository
                .findByInterviewIdAndInterviewerIdWithEntries(interviewId, interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Scorecard not found for this interview/interviewer"));
        return toScorecardResponse(scorecard);
    }

    @Transactional(readOnly = true)
    public List<ScorecardResponse> getScorecardsByInterviewer(UUID interviewerId) {
        return scorecardRepository.findByInterviewerIdWithEntries(interviewerId).stream()
                .map(this::toScorecardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScorecardResponse> getScorecardsByCandidate(UUID candidateId) {
        return scorecardRepository.findByCandidateIdWithEntries(candidateId).stream()
                .map(this::toScorecardResponse)
                .toList();
    }

    // ==================== Aggregated Summary ====================

    @Transactional(readOnly = true)
    public CandidateScorecardSummary getCandidateSummary(UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        List<EvaluationScorecard> scorecards = scorecardRepository.findByInterviewIdWithEntries(interviewId);

        if (scorecards.isEmpty()) {
            return CandidateScorecardSummary.builder()
                    .candidateId(interview.getCandidate().getId())
                    .candidateName(interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName())
                    .interviewId(interviewId)
                    .interviewTitle(interview.getTitle())
                    .averageOverallScore(0.0)
                    .totalScorecards(0)
                    .averageScoreByCriteria(Map.of())
                    .recommendationBreakdown(Map.of())
                    .build();
        }

        // Average overall score
        double avgScore = scorecards.stream()
                .mapToDouble(EvaluationScorecard::getOverallScore)
                .average()
                .orElse(0.0);

        // Recommendation breakdown
        Map<FeedbackRecommendation, Integer> recommendations = scorecards.stream()
                .filter(s -> s.getRecommendation() != null)
                .collect(Collectors.groupingBy(
                        EvaluationScorecard::getRecommendation,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Average score per criteria
        Map<String, List<Integer>> scoresByCriteria = new LinkedHashMap<>();
        for (EvaluationScorecard sc : scorecards) {
            for (ScorecardEntry entry : sc.getEntries()) {
                scoresByCriteria
                        .computeIfAbsent(entry.getCriteria().getName(), k -> new ArrayList<>())
                        .add(entry.getScore());
            }
        }

        Map<String, Double> avgByCriteria = new LinkedHashMap<>();
        scoresByCriteria.forEach((name, scores) -> {
            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            avgByCriteria.put(name, Math.round(avg * 10.0) / 10.0);
        });

        return CandidateScorecardSummary.builder()
                .candidateId(interview.getCandidate().getId())
                .candidateName(interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName())
                .interviewId(interviewId)
                .interviewTitle(interview.getTitle())
                .averageOverallScore(Math.round(avgScore * 10.0) / 10.0)
                .totalScorecards(scorecards.size())
                .averageScoreByCriteria(avgByCriteria)
                .recommendationBreakdown(recommendations)
                .build();
    }

    // ==================== Helpers ====================

    private CriteriaResponse toCriteriaResponse(EvaluationCriteria c) {
        return CriteriaResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .interviewType(c.getInterviewType())
                .maxScore(c.getMaxScore())
                .weight(c.getWeight())
                .orderIndex(c.getOrderIndex())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private ScorecardResponse toScorecardResponse(EvaluationScorecard s) {
        List<ScoreEntryResponse> entries = s.getEntries().stream()
                .map(e -> ScoreEntryResponse.builder()
                        .id(e.getId())
                        .criteriaId(e.getCriteria().getId())
                        .criteriaName(e.getCriteria().getName())
                        .criteriaDescription(e.getCriteria().getDescription())
                        .maxScore(e.getCriteria().getMaxScore())
                        .weight(e.getCriteria().getWeight())
                        .score(e.getScore())
                        .comments(e.getComments())
                        .build())
                .toList();

        return ScorecardResponse.builder()
                .id(s.getId())
                .interviewId(s.getInterview().getId())
                .interviewTitle(s.getInterview().getTitle())
                .interviewerId(s.getInterviewer().getId())
                .interviewerName(s.getInterviewer().getFirstName() + " " + s.getInterviewer().getLastName())
                .candidateId(s.getInterview().getCandidate().getId())
                .candidateName(s.getInterview().getCandidate().getFirstName() + " " + s.getInterview().getCandidate().getLastName())
                .overallScore(s.getOverallScore())
                .recommendation(s.getRecommendation())
                .overallComments(s.getOverallComments())
                .strengths(s.getStrengths())
                .weaknesses(s.getWeaknesses())
                .entries(entries)
                .submittedAt(s.getSubmittedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}


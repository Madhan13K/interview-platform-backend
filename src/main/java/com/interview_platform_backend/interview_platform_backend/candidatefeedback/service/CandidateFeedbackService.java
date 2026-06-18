package com.interview_platform_backend.interview_platform_backend.candidatefeedback.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.CandidateFeedbackResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.FeedbackSummaryResponse;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto.SubmitCandidateFeedbackRequest;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.entity.CandidateFeedback;
import com.interview_platform_backend.interview_platform_backend.candidatefeedback.repository.CandidateFeedbackRepository;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CandidateFeedbackService {

    private final CandidateFeedbackRepository candidateFeedbackRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public CandidateFeedbackService(CandidateFeedbackRepository candidateFeedbackRepository,
                                    InterviewRepository interviewRepository,
                                    UserRepository userRepository) {
        this.candidateFeedbackRepository = candidateFeedbackRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    public CandidateFeedbackResponse submitFeedback(SubmitCandidateFeedbackRequest request, UUID userId) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", request.getInterviewId()));

        User candidate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (candidateFeedbackRepository.existsByInterviewIdAndCandidateId(request.getInterviewId(), userId)) {
            throw new DuplicateResourceException("Feedback already submitted for this interview by this candidate");
        }

        CandidateFeedback feedback = CandidateFeedback.builder()
                .organizationId(null)
                .interview(interview)
                .candidate(candidate)
                .overallRating(request.getOverallRating())
                .communicationRating(request.getCommunicationRating())
                .professionalismRating(request.getProfessionalismRating())
                .technicalClarityRating(request.getTechnicalClarityRating())
                .timelinessRating(request.getTimelinessRating())
                .comments(request.getComments())
                .wouldRecommend(request.getWouldRecommend())
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .build();

        CandidateFeedback saved = candidateFeedbackRepository.save(feedback);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CandidateFeedbackResponse> getFeedbackByInterview(UUID interviewId) {
        if (!interviewRepository.existsById(interviewId)) {
            throw new ResourceNotFoundException("Interview", "id", interviewId);
        }

        return candidateFeedbackRepository.findByInterviewId(interviewId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidateFeedbackResponse> getFeedbackByCandidate(UUID candidateId) {
        if (!userRepository.existsById(candidateId)) {
            throw new ResourceNotFoundException("User", "id", candidateId);
        }

        return candidateFeedbackRepository.findByCandidateId(candidateId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryResponse getFeedbackSummary() {
        long totalResponses = candidateFeedbackRepository.count();

        if (totalResponses == 0) {
            return FeedbackSummaryResponse.builder()
                    .totalResponses(0L)
                    .averageOverallRating(0.0)
                    .averageCommunicationRating(0.0)
                    .averageProfessionalismRating(0.0)
                    .averageTechnicalClarityRating(0.0)
                    .averageTimelinessRating(0.0)
                    .recommendationRate(0.0)
                    .build();
        }

        Double avgOverall = candidateFeedbackRepository.findAverageOverallRating();
        Double avgCommunication = candidateFeedbackRepository.findAverageCommunicationRating();
        Double avgProfessionalism = candidateFeedbackRepository.findAverageProfessionalismRating();
        Double avgTechnicalClarity = candidateFeedbackRepository.findAverageTechnicalClarityRating();
        Double avgTimeliness = candidateFeedbackRepository.findAverageTimelinessRating();
        Long recommendCount = candidateFeedbackRepository.countByWouldRecommendTrue();

        double recommendationRate = (recommendCount * 100.0) / totalResponses;

        return FeedbackSummaryResponse.builder()
                .totalResponses(totalResponses)
                .averageOverallRating(avgOverall)
                .averageCommunicationRating(avgCommunication)
                .averageProfessionalismRating(avgProfessionalism)
                .averageTechnicalClarityRating(avgTechnicalClarity)
                .averageTimelinessRating(avgTimeliness)
                .recommendationRate(recommendationRate)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CandidateFeedbackResponse> getMyFeedback(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CandidateFeedback> feedbackPage = candidateFeedbackRepository.findByCandidateId(userId, pageable);

        List<CandidateFeedbackResponse> content = feedbackPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PaginatedResponse.<CandidateFeedbackResponse>builder()
                .content(content)
                .page(feedbackPage.getNumber())
                .size(feedbackPage.getSize())
                .totalElements(feedbackPage.getTotalElements())
                .totalPages(feedbackPage.getTotalPages())
                .last(feedbackPage.isLast())
                .build();
    }

    private CandidateFeedbackResponse toResponse(CandidateFeedback feedback) {
        String candidateName = null;
        if (!Boolean.TRUE.equals(feedback.getIsAnonymous())) {
            User candidate = feedback.getCandidate();
            candidateName = candidate.getFirstName() + " " + candidate.getLastName();
        }

        return CandidateFeedbackResponse.builder()
                .id(feedback.getId())
                .interviewId(feedback.getInterview().getId())
                .candidateId(feedback.getCandidate().getId())
                .candidateName(candidateName)
                .overallRating(feedback.getOverallRating())
                .communicationRating(feedback.getCommunicationRating())
                .professionalismRating(feedback.getProfessionalismRating())
                .technicalClarityRating(feedback.getTechnicalClarityRating())
                .timelinessRating(feedback.getTimelinessRating())
                .comments(feedback.getComments())
                .wouldRecommend(feedback.getWouldRecommend())
                .isAnonymous(feedback.getIsAnonymous())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}

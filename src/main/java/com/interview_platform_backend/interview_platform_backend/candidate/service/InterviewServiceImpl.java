package com.interview_platform_backend.interview_platform_backend.candidate.service;

import com.interview_platform_backend.interview_platform_backend.audit.AuditAction;
import com.interview_platform_backend.interview_platform_backend.audit.AuditService;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.*;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.*;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewFeedbackRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewInterviewerRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.event.FeedbackSubmittedEvent;
import com.interview_platform_backend.interview_platform_backend.event.InterviewCancelledEvent;
import com.interview_platform_backend.interview_platform_backend.event.InterviewRescheduledEvent;
import com.interview_platform_backend.interview_platform_backend.event.InterviewScheduledEvent;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewInterviewerRepository interviewInterviewerRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    public InterviewServiceImpl(
            InterviewRepository interviewRepository,
            InterviewInterviewerRepository interviewInterviewerRepository,
            InterviewFeedbackRepository feedbackRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService
    ) {
        this.interviewRepository = interviewRepository;
        this.interviewInterviewerRepository = interviewInterviewerRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "interviews", allEntries = true),
            @CacheEvict(value = "interviewsByCandidate", allEntries = true),
            @CacheEvict(value = "interviewsByInterviewer", allEntries = true)
    })
    public InterviewResponse createInterview(CreateInterviewRequest request, UUID scheduledByUserId) {
        User candidate = userRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", request.getCandidateId()));

        User scheduledBy = userRepository.findById(scheduledByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", scheduledByUserId));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Interview interview = Interview.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .candidate(candidate)
                .scheduledBy(scheduledBy)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timeZone(request.getTimeZone())
                .type(request.getType())
                .mode(request.getMode())
                .meetingLink(request.getMeetingLink())
                .location(request.getLocation())
                .status(InterviewStatus.SCHEDULED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        List<InterviewInterviewer> assignments = new java.util.ArrayList<>(request.getInterviewerIds().stream()
                .distinct()
                .map(interviewerId -> {
                    User interviewer = userRepository.findById(interviewerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));
                    return InterviewInterviewer.builder()
                            .interview(interview)
                            .interviewer(interviewer)
                            .isPrimaryInterviewer(false)
                            .assignedAt(Instant.now())
                            .build();
                })
                .toList());

        interview.setInterviewers(assignments);
        Interview saved = interviewRepository.save(interview);

        // Publish event for notifications
        List<String> interviewerEmails = assignments.stream()
                .map(a -> a.getInterviewer().getEmail())
                .toList();
        eventPublisher.publishEvent(new InterviewScheduledEvent(this,
                saved.getId(), saved.getTitle(),
                candidate.getEmail(), candidate.getFirstName() + " " + candidate.getLastName(),
                interviewerEmails, saved.getStartTime(), saved.getEndTime(),
                scheduledBy.getFirstName() + " " + scheduledBy.getLastName()));

        auditService.log("INTERVIEW", saved.getId(), AuditAction.CREATE,
                "Interview scheduled: " + saved.getTitle());

        return toResponse(saved);
    }

    @Override
    @Cacheable(value = "interviews", key = "#interviewId")
    @Transactional(readOnly = true)
    public InterviewResponse getInterview(UUID interviewId) {
        Interview interview = findInterviewWithDetails(interviewId);
        return toResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviews() {
        return interviewRepository.findAllWithDetails()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "interviews", allEntries = true),
            @CacheEvict(value = "interviewsByCandidate", allEntries = true),
            @CacheEvict(value = "interviewsByInterviewer", allEntries = true)
    })
    public InterviewResponse updateInterview(UUID interviewId, UpdateInterviewRequest request) {
        Interview interview = findInterviewWithDetails(interviewId);

        if (interview.getStatus() == InterviewStatus.CANCELLED || interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new BadRequestException("Cannot update a " + interview.getStatus().name().toLowerCase() + " interview");
        }

        if (request.getTitle() != null) interview.setTitle(request.getTitle());
        if (request.getDescription() != null) interview.setDescription(request.getDescription());
        if (request.getTimeZone() != null) interview.setTimeZone(request.getTimeZone());
        if (request.getType() != null) interview.setType(request.getType());
        if (request.getMode() != null) interview.setMode(request.getMode());
        if (request.getMeetingLink() != null) interview.setMeetingLink(request.getMeetingLink());
        if (request.getLocation() != null) interview.setLocation(request.getLocation());

        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (!request.getEndTime().isAfter(request.getStartTime())) {
                throw new BadRequestException("End time must be after start time");
            }
            interview.setStartTime(request.getStartTime());
            interview.setEndTime(request.getEndTime());
            interview.setStatus(InterviewStatus.RESCHEDULED);
            interview.setRescheduleReason("Interview rescheduled");

            // Publish reschedule event
            List<String> emails = interview.getInterviewers().stream()
                    .map(ii -> ii.getInterviewer().getEmail()).toList();
            eventPublisher.publishEvent(new InterviewRescheduledEvent(this,
                    interview.getId(), interview.getTitle(),
                    interview.getCandidate().getEmail(),
                    interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName(),
                    emails, request.getStartTime(), request.getEndTime(),
                    "Interview rescheduled"));
        } else if (request.getStartTime() != null) {
            interview.setStartTime(request.getStartTime());
        } else if (request.getEndTime() != null) {
            interview.setEndTime(request.getEndTime());
        }

        if (request.getInterviewerIds() != null && !request.getInterviewerIds().isEmpty()) {
            List<InterviewInterviewer> newAssignments = new java.util.ArrayList<>(request.getInterviewerIds().stream()
                    .distinct()
                    .map(interviewerId -> {
                        User interviewer = userRepository.findById(interviewerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));
                        return InterviewInterviewer.builder()
                                .interview(interview)
                                .interviewer(interviewer)
                                .isPrimaryInterviewer(false)
                                .assignedAt(Instant.now())
                                .build();
                    })
                    .toList());
            interview.getInterviewers().addAll(newAssignments);
        }

        interview.setUpdatedAt(Instant.now());
        Interview saved = interviewRepository.save(interview);
        return toResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "interviews", allEntries = true),
            @CacheEvict(value = "interviewsByCandidate", allEntries = true),
            @CacheEvict(value = "interviewsByInterviewer", allEntries = true)
    })
    public InterviewResponse cancelInterview(UUID interviewId, CancelInterviewRequest request) {
        Interview interview = findInterviewWithDetails(interviewId);

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new BadRequestException("Interview is already cancelled");
        }
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed interview");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancelReason(request.getReason());
        interview.setUpdatedAt(Instant.now());

        Interview saved = interviewRepository.save(interview);

        // Publish cancellation event
        List<String> interviewerEmails = interview.getInterviewers().stream()
                .map(ii -> ii.getInterviewer().getEmail())
                .toList();
        eventPublisher.publishEvent(new InterviewCancelledEvent(this,
                saved.getId(), saved.getTitle(),
                saved.getCandidate().getEmail(),
                saved.getCandidate().getFirstName() + " " + saved.getCandidate().getLastName(),
                interviewerEmails, request.getReason()));

        auditService.log("INTERVIEW", saved.getId(), AuditAction.STATUS_CHANGE,
                "Interview cancelled. Reason: " + request.getReason());

        return toResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "interviews", allEntries = true),
            @CacheEvict(value = "interviewsByCandidate", allEntries = true),
            @CacheEvict(value = "interviewsByInterviewer", allEntries = true)
    })
    public InterviewResponse updateStatus(UUID interviewId, InterviewStatus status) {
        Interview interview = findInterviewWithDetails(interviewId);

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a cancelled interview");
        }

        interview.setStatus(status);
        interview.setUpdatedAt(Instant.now());

        Interview saved = interviewRepository.save(interview);
        return toResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "interviews", allEntries = true),
            @CacheEvict(value = "interviewsByCandidate", allEntries = true),
            @CacheEvict(value = "interviewsByInterviewer", allEntries = true)
    })
    public void deleteInterview(UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));
        interviewRepository.delete(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getMyInterviewsAsCandidate(UUID userId) {
        return interviewRepository.findByCandidateId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getMyInterviewsAsInterviewer(UUID userId) {
        return interviewRepository.findByInterviewerId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public InterviewResponse addInterviewer(UUID interviewId, UUID interviewerId, boolean isPrimary) {
        Interview interview = findInterviewWithDetails(interviewId);
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));

        boolean alreadyAssigned = interview.getInterviewers().stream()
                .anyMatch(ii -> ii.getInterviewer().getId().equals(interviewerId));
        if (alreadyAssigned) {
            throw new DuplicateResourceException("Interviewer already assigned to this interview");
        }

        InterviewInterviewer assignment = InterviewInterviewer.builder()
                .interview(interview)
                .interviewer(interviewer)
                .isPrimaryInterviewer(isPrimary)
                .assignedAt(Instant.now())
                .build();

        interview.getInterviewers().add(assignment);
        interview.setUpdatedAt(Instant.now());
        Interview saved = interviewRepository.save(interview);
        return toResponse(saved);
    }

    @Override
    public InterviewResponse removeInterviewer(UUID interviewId, UUID interviewerId) {
        Interview interview = findInterviewWithDetails(interviewId);

        InterviewInterviewer assignment = interviewInterviewerRepository
                .findByInterviewAndInterviewer(interview,
                        userRepository.findById(interviewerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId)))
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer assignment not found"));

        interview.getInterviewers().remove(assignment);
        interviewInterviewerRepository.delete(assignment);
        interview.setUpdatedAt(Instant.now());

        Interview saved = interviewRepository.save(interview);
        return toResponse(saved);
    }

    @Override
    public FeedbackResponse submitFeedback(UUID interviewId, UUID interviewerId, SubmitFeedbackRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));

        if (feedbackRepository.existsByInterviewAndInterviewer(interview, interviewer)) {
            throw new DuplicateResourceException("Feedback already submitted for this interview by this interviewer");
        }

        if (interview.getStatus() != InterviewStatus.COMPLETED && interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BadRequestException("Feedback can only be submitted for completed or in-progress interviews");
        }

        InterviewFeedBack feedback = InterviewFeedBack.builder()
                .interview(interview)
                .interviewer(interviewer)
                .rating(request.getRating())
                .recommendation(request.getRecommendation())
                .strengths(request.getStrengths())
                .weaknesses(request.getWeaknesses())
                .comments(request.getComments())
                .submittedAt(Instant.now())
                .build();

        InterviewFeedBack saved = feedbackRepository.save(feedback);

        // Publish feedback event
        eventPublisher.publishEvent(new FeedbackSubmittedEvent(this,
                interview.getId(), interview.getTitle(),
                interviewer.getFirstName() + " " + interviewer.getLastName(),
                interviewer.getEmail(),
                interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName(),
                request.getRating(), request.getRecommendation()));

        auditService.log("INTERVIEW_FEEDBACK", saved.getId(), AuditAction.SUBMIT_FEEDBACK,
                "Feedback submitted: rating=" + request.getRating() + ", recommendation=" + request.getRecommendation());

        return toFeedbackResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getInterviewFeedback(UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        return feedbackRepository.findByInterview(interview)
                .stream()
                .map(this::toFeedbackResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackByInterviewer(UUID interviewId, UUID interviewerId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        InterviewFeedBack feedback = feedbackRepository.findByInterviewAndInterviewer(interview, interviewer)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "interviewerId", interviewerId));

        return toFeedbackResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getAllFeedbackByInterviewer(UUID interviewerId) {
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        return feedbackRepository.findByInterviewer(interviewer)
                .stream()
                .map(this::toFeedbackResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByStatus(InterviewStatus status) {
        return interviewRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<InterviewResponse> getInterviewsByStatusPaginated(InterviewStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> idPage = interviewRepository.findIdsByStatus(status, pageable);

        List<InterviewResponse> content = idPage.getContent().isEmpty()
                ? List.of()
                : interviewRepository.findAllWithDetailsByIds(idPage.getContent())
                    .stream().map(this::toResponse).toList();

        return PaginatedResponse.<InterviewResponse>builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .last(idPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByDateRange(Instant from, Instant to) {
        return interviewRepository.findByDateRange(from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<InterviewResponse> getInterviewsByDateRangePaginated(Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> idPage = interviewRepository.findIdsByDateRange(from, to, pageable);

        List<InterviewResponse> content = idPage.getContent().isEmpty()
                ? List.of()
                : interviewRepository.findAllWithDetailsByIds(idPage.getContent())
                    .stream().map(this::toResponse).toList();

        return PaginatedResponse.<InterviewResponse>builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .last(idPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<InterviewResponse> getInterviewsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> idPage = interviewRepository.findAllIds(pageable);

        List<InterviewResponse> content = idPage.getContent().isEmpty()
                ? List.of()
                : interviewRepository.findAllWithDetailsByIds(idPage.getContent())
                    .stream().map(this::toResponse).toList();

        return PaginatedResponse.<InterviewResponse>builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .last(idPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<InterviewResponse> getMyInterviewsAsCandidatePaginated(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> idPage = interviewRepository.findIdsByCandidateId(userId, pageable);

        List<InterviewResponse> content = idPage.getContent().isEmpty()
                ? List.of()
                : interviewRepository.findAllWithDetailsByIds(idPage.getContent())
                    .stream().map(this::toResponse).toList();

        return PaginatedResponse.<InterviewResponse>builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .last(idPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<InterviewResponse> getMyInterviewsAsInterviewerPaginated(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> idPage = interviewRepository.findIdsByInterviewerId(userId, pageable);

        List<InterviewResponse> content = idPage.getContent().isEmpty()
                ? List.of()
                : interviewRepository.findAllWithDetailsByIds(idPage.getContent())
                    .stream().map(this::toResponse).toList();

        return PaginatedResponse.<InterviewResponse>builder()
                .content(content)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .last(idPage.isLast())
                .build();
    }

    // ---- Helper methods ----

    private Interview findInterviewWithDetails(UUID interviewId) {
        return interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));
    }

    private InterviewResponse toResponse(Interview interview) {
        InterviewResponse response = new InterviewResponse();
        response.setId(interview.getId());
        response.setTitle(interview.getTitle());
        response.setDescription(interview.getDescription());

        response.setCandidateId(interview.getCandidate().getId());
        response.setCandidateName(interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName());
        response.setCandidateEmail(interview.getCandidate().getEmail());

        response.setScheduledById(interview.getScheduledBy().getId());
        response.setScheduledByName(interview.getScheduledBy().getFirstName() + " " + interview.getScheduledBy().getLastName());

        response.setStartTime(interview.getStartTime());
        response.setEndTime(interview.getEndTime());
        response.setTimeZone(interview.getTimeZone());

        response.setStatus(interview.getStatus());
        response.setType(interview.getType());
        response.setMode(interview.getMode());

        response.setMeetingLink(interview.getMeetingLink());
        response.setLocation(interview.getLocation());
        response.setCancelReason(interview.getCancelReason());
        response.setRescheduleReason(interview.getRescheduleReason());
        response.setCreatedAt(interview.getCreatedAt());
        response.setUpdatedAt(interview.getUpdatedAt());

        if (interview.getInterviewers() != null) {
            List<InterviewInterviewerResponse> interviewerResponses = interview.getInterviewers()
                    .stream()
                    .map(assignment -> {
                        InterviewInterviewerResponse ir = new InterviewInterviewerResponse();
                        ir.setInterviewerId(assignment.getInterviewer().getId());
                        ir.setFirstName(assignment.getInterviewer().getFirstName());
                        ir.setLastName(assignment.getInterviewer().getLastName());
                        ir.setEmail(assignment.getInterviewer().getEmail());
                        ir.setPrimaryInterviewer(assignment.isPrimaryInterviewer());
                        return ir;
                    })
                    .toList();
            response.setInterviewers(interviewerResponses);
        }

        return response;
    }

    private FeedbackResponse toFeedbackResponse(InterviewFeedBack feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .interviewId(feedback.getInterview().getId())
                .interviewerId(feedback.getInterviewer().getId())
                .interviewerName(feedback.getInterviewer().getFirstName() + " " + feedback.getInterviewer().getLastName())
                .interviewerEmail(feedback.getInterviewer().getEmail())
                .rating(feedback.getRating())
                .recommendation(feedback.getRecommendation())
                .strengths(feedback.getStrengths())
                .weaknesses(feedback.getWeaknesses())
                .comments(feedback.getComments())
                .submittedAt(feedback.getSubmittedAt())
                .build();
    }
}

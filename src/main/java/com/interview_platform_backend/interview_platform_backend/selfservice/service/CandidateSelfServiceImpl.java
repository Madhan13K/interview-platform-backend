package com.interview_platform_backend.interview_platform_backend.selfservice.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.jobposition.repository.JobPositionRepository;
import com.interview_platform_backend.interview_platform_backend.selfservice.dto.*;
import com.interview_platform_backend.interview_platform_backend.selfservice.entity.CandidatePreferredSlot;
import com.interview_platform_backend.interview_platform_backend.selfservice.repository.CandidatePreferredSlotRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CandidateSelfServiceImpl {

    private final CandidatePreferredSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final JobPositionRepository jobPositionRepository;

    public CandidateSelfServiceImpl(CandidatePreferredSlotRepository slotRepository,
                                     UserRepository userRepository,
                                     InterviewRepository interviewRepository,
                                     JobPositionRepository jobPositionRepository) {
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.interviewRepository = interviewRepository;
        this.jobPositionRepository = jobPositionRepository;
    }

    public PreferredSlotResponse submitPreferredSlot(SubmitPreferredSlotRequest request, UUID candidateId) {
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", candidateId));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        CandidatePreferredSlot slot = CandidatePreferredSlot.builder()
                .candidate(candidate)
                .preferredDate(request.getPreferredDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timeZone(request.getTimeZone() != null ? request.getTimeZone() : "UTC")
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .notes(request.getNotes())
                .status(CandidatePreferredSlot.SlotStatus.SUBMITTED)
                .build();

        if (request.getInterviewId() != null) {
            Interview interview = interviewRepository.findById(request.getInterviewId())
                    .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", request.getInterviewId()));
            slot.setInterview(interview);
        }

        if (request.getJobPositionId() != null) {
            JobPosition jp = jobPositionRepository.findById(request.getJobPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", request.getJobPositionId()));
            slot.setJobPosition(jp);
        }

        CandidatePreferredSlot saved = slotRepository.save(slot);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PreferredSlotResponse> getMyPreferredSlots(UUID candidateId) {
        return slotRepository.findByCandidateIdOrderByPriorityAsc(candidateId).stream()
                .map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PreferredSlotResponse> getSlotsByInterview(UUID interviewId) {
        return slotRepository.findByInterviewId(interviewId).stream()
                .map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PreferredSlotResponse> getSlotsByJobPosition(UUID jobPositionId) {
        return slotRepository.findByJobPositionId(jobPositionId).stream()
                .map(this::mapToResponse).toList();
    }

    public PreferredSlotResponse updateSlotStatus(UUID slotId, CandidatePreferredSlot.SlotStatus status) {
        CandidatePreferredSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidatePreferredSlot", "id", slotId));
        slot.setStatus(status);
        return mapToResponse(slotRepository.save(slot));
    }

    public void deleteSlot(UUID slotId, UUID candidateId) {
        CandidatePreferredSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidatePreferredSlot", "id", slotId));
        if (!slot.getCandidate().getId().equals(candidateId)) {
            throw new BadRequestException("You can only delete your own preferred slots");
        }
        slotRepository.delete(slot);
    }

    private PreferredSlotResponse mapToResponse(CandidatePreferredSlot slot) {
        return PreferredSlotResponse.builder()
                .id(slot.getId())
                .candidateId(slot.getCandidate().getId())
                .candidateName(slot.getCandidate().getFirstName() + " " + slot.getCandidate().getLastName())
                .interviewId(slot.getInterview() != null ? slot.getInterview().getId() : null)
                .jobPositionId(slot.getJobPosition() != null ? slot.getJobPosition().getId() : null)
                .preferredDate(slot.getPreferredDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .timeZone(slot.getTimeZone())
                .priority(slot.getPriority())
                .notes(slot.getNotes())
                .status(slot.getStatus())
                .createdAt(slot.getCreatedAt())
                .build();
    }
}


package com.interview_platform_backend.interview_platform_backend.meeting.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.meeting.dto.GenerateMeetingRequest;
import com.interview_platform_backend.interview_platform_backend.meeting.dto.MeetingLinkResponse;
import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingLink;
import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingProvider;
import com.interview_platform_backend.interview_platform_backend.meeting.provider.MeetingProviderStrategy;
import com.interview_platform_backend.interview_platform_backend.meeting.repository.MeetingLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class MeetingService {

    private final MeetingLinkRepository meetingLinkRepository;
    private final InterviewRepository interviewRepository;
    private final Map<MeetingProvider, MeetingProviderStrategy> providers;

    public MeetingService(MeetingLinkRepository meetingLinkRepository,
                          InterviewRepository interviewRepository,
                          List<MeetingProviderStrategy> providerStrategies) {
        this.meetingLinkRepository = meetingLinkRepository;
        this.interviewRepository = interviewRepository;
        this.providers = providerStrategies.stream()
                .collect(Collectors.toMap(MeetingProviderStrategy::getProviderType, Function.identity()));
    }

    public MeetingLinkResponse generateMeetingLink(UUID interviewId, GenerateMeetingRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (meetingLinkRepository.existsByInterviewId(interviewId)) {
            throw new DuplicateResourceException("Meeting link already exists for this interview");
        }

        MeetingProvider provider = request.getProvider();
        MeetingProviderStrategy strategy = providers.get(provider);
        if (strategy == null) {
            // Fallback to INTERNAL if requested provider is not available
            strategy = providers.get(MeetingProvider.INTERNAL);
            provider = MeetingProvider.INTERNAL;
        }

        String topic = request.getTopic() != null ? request.getTopic() : interview.getTitle();
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 60;

        MeetingProviderStrategy.MeetingDetails details = strategy.generateMeeting(
                topic, interview.getStartTime(), interview.getEndTime(), duration);

        MeetingLink meetingLink = MeetingLink.builder()
                .interview(interview)
                .provider(provider)
                .meetingUrl(details.meetingUrl())
                .hostUrl(details.hostUrl())
                .meetingId(details.meetingId())
                .passcode(details.passcode())
                .expiresAt(details.expiresAt())
                .build();

        MeetingLink saved = meetingLinkRepository.save(meetingLink);

        // Also update the interview's meetingLink field
        interview.setMeetingLink(details.meetingUrl());
        interviewRepository.save(interview);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MeetingLinkResponse getMeetingLink(UUID interviewId) {
        MeetingLink link = meetingLinkRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("MeetingLink", "interviewId", interviewId));
        return toResponse(link);
    }

    private MeetingLinkResponse toResponse(MeetingLink entity) {
        return MeetingLinkResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterview().getId())
                .provider(entity.getProvider())
                .meetingUrl(entity.getMeetingUrl())
                .hostUrl(entity.getHostUrl())
                .meetingId(entity.getMeetingId())
                .passcode(entity.getPasscode())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }
}


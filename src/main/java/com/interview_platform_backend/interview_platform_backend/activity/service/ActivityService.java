package com.interview_platform_backend.interview_platform_backend.activity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview_platform_backend.interview_platform_backend.activity.dto.ActivityEventResponse;
import com.interview_platform_backend.interview_platform_backend.activity.dto.ActivityFilterRequest;
import com.interview_platform_backend.interview_platform_backend.activity.entity.ActivityEvent;
import com.interview_platform_backend.interview_platform_backend.activity.repository.ActivityEventRepository;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ActivityService {

    private final ActivityEventRepository activityEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ActivityService(ActivityEventRepository activityEventRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.activityEventRepository = activityEventRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public ActivityEventResponse logActivity(UUID actorId, String action, String entityType,
                                             UUID entityId, String targetType, UUID targetId,
                                             Map<String, Object> metadata) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", actorId));

        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                metadataJson = "{}";
            }
        }

        ActivityEvent event = ActivityEvent.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(metadataJson)
                .build();

        ActivityEvent saved = activityEventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityEventResponse> getActivityForEntity(String entityType, UUID entityId) {
        return activityEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ActivityEventResponse> getActivityByActor(UUID actorId, int page, int size) {
        if (!userRepository.existsById(actorId)) {
            throw new ResourceNotFoundException("User", "id", actorId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityEvent> eventPage = activityEventRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable);

        return toPaginatedResponse(eventPage);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ActivityEventResponse> getActivityFeed(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityEvent> eventPage = activityEventRepository.findAllByOrderByCreatedAtDesc(pageable);

        return toPaginatedResponse(eventPage);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ActivityEventResponse> getFilteredActivity(ActivityFilterRequest filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityEvent> eventPage = activityEventRepository.findWithFilters(
                filter.getEntityType(),
                filter.getEntityId(),
                filter.getActorId(),
                filter.getAction(),
                filter.getStartDate(),
                filter.getEndDate(),
                pageable);

        return toPaginatedResponse(eventPage);
    }

    // Convenience methods

    public void logInterviewCreated(UUID actorId, UUID interviewId, String interviewTitle) {
        logActivity(actorId, "CREATED", "INTERVIEW", interviewId, null, null,
                Map.of("title", interviewTitle));
    }

    public void logInterviewScheduled(UUID actorId, UUID interviewId, String interviewTitle) {
        logActivity(actorId, "SCHEDULED", "INTERVIEW", interviewId, null, null,
                Map.of("title", interviewTitle));
    }

    public void logFeedbackSubmitted(UUID actorId, UUID feedbackId, UUID interviewId) {
        logActivity(actorId, "SUBMITTED", "FEEDBACK", feedbackId, "INTERVIEW", interviewId, null);
    }

    public void logCandidateAdvanced(UUID actorId, UUID candidateId, String stageName) {
        logActivity(actorId, "ADVANCED", "CANDIDATE", candidateId, null, null,
                Map.of("stage", stageName));
    }

    // Helper methods

    private PaginatedResponse<ActivityEventResponse> toPaginatedResponse(Page<ActivityEvent> eventPage) {
        List<ActivityEventResponse> content = eventPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PaginatedResponse.<ActivityEventResponse>builder()
                .content(content)
                .page(eventPage.getNumber())
                .size(eventPage.getSize())
                .totalElements(eventPage.getTotalElements())
                .totalPages(eventPage.getTotalPages())
                .last(eventPage.isLast())
                .build();
    }

    private ActivityEventResponse toResponse(ActivityEvent event) {
        User actor = event.getActor();
        Map<String, Object> metadataMap = parseMetadata(event.getMetadata());

        return ActivityEventResponse.builder()
                .id(event.getId())
                .actorId(actor.getId())
                .actorName(actor.getFirstName() + " " + actor.getLastName())
                .actorEmail(actor.getEmail())
                .action(event.getAction())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .metadata(metadataMap)
                .createdAt(event.getCreatedAt())
                .build();
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}

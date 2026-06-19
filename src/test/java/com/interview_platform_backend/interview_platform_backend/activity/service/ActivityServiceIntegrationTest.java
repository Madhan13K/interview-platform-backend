package com.interview_platform_backend.interview_platform_backend.activity.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.activity.dto.ActivityEventResponse;
import com.interview_platform_backend.interview_platform_backend.activity.dto.ActivityFilterRequest;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class ActivityServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserRepository userRepository;

    private User actor;

    @BeforeEach
    void setUp() {
        actor = userRepository.save(User.builder()
                .firstName("Actor")
                .lastName("Activity")
                .email("act-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
    }

    @Nested
    @DisplayName("Log Activity")
    class LogActivity {

        @Test
        @DisplayName("should log activity successfully")
        void logActivity_success() {
            UUID entityId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            Map<String, Object> metadata = Map.of("key", "value", "count", 42);

            ActivityEventResponse response = activityService.logActivity(
                    actor.getId(), "CREATED", "INTERVIEW", entityId, "CANDIDATE", targetId, metadata);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getActorId()).isEqualTo(actor.getId());
            assertThat(response.getActorName()).isEqualTo("Actor Activity");
            assertThat(response.getActorEmail()).isEqualTo(actor.getEmail());
            assertThat(response.getAction()).isEqualTo("CREATED");
            assertThat(response.getEntityType()).isEqualTo("INTERVIEW");
            assertThat(response.getEntityId()).isEqualTo(entityId);
            assertThat(response.getTargetType()).isEqualTo("CANDIDATE");
            assertThat(response.getTargetId()).isEqualTo(targetId);
            assertThat(response.getMetadata()).containsEntry("key", "value");
            assertThat(response.getMetadata()).containsEntry("count", 42);
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should log activity with null metadata")
        void logActivity_nullMetadata() {
            UUID entityId = UUID.randomUUID();

            ActivityEventResponse response = activityService.logActivity(
                    actor.getId(), "DELETED", "INTERVIEW", entityId, null, null, null);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getAction()).isEqualTo("DELETED");
            assertThat(response.getTargetType()).isNull();
            assertThat(response.getTargetId()).isNull();
            assertThat(response.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when actor not found")
        void logActivity_actorNotFound() {
            UUID unknownActorId = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();

            assertThatThrownBy(() -> activityService.logActivity(
                    unknownActorId, "CREATED", "INTERVIEW", entityId, null, null, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Activity For Entity")
    class GetActivityForEntity {

        @Test
        @DisplayName("should return activity events for entity")
        void getActivityForEntity_success() {
            UUID entityId = UUID.randomUUID();
            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", entityId, null, null, null);
            activityService.logActivity(actor.getId(), "UPDATED", "INTERVIEW", entityId, null, null, null);

            List<ActivityEventResponse> results = activityService.getActivityForEntity("INTERVIEW", entityId);

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(r -> r.getEntityType().equals("INTERVIEW"));
            assertThat(results).allMatch(r -> r.getEntityId().equals(entityId));
        }

        @Test
        @DisplayName("should return empty list when no events exist")
        void getActivityForEntity_empty() {
            List<ActivityEventResponse> results = activityService
                    .getActivityForEntity("INTERVIEW", UUID.randomUUID());

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should not return events for different entity type")
        void getActivityForEntity_differentType() {
            UUID entityId = UUID.randomUUID();
            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", entityId, null, null, null);

            List<ActivityEventResponse> results = activityService.getActivityForEntity("CANDIDATE", entityId);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Activity By Actor")
    class GetActivityByActor {

        @Test
        @DisplayName("should return paginated activity for actor")
        void getActivityByActor_paginated() {
            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", UUID.randomUUID(), null, null, null);
            activityService.logActivity(actor.getId(), "UPDATED", "CANDIDATE", UUID.randomUUID(), null, null, null);

            PaginatedResponse<ActivityEventResponse> result = activityService
                    .getActivityByActor(actor.getId(), 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(r -> r.getActorId().equals(actor.getId()));
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when actor not found")
        void getActivityByActor_notFound() {
            assertThatThrownBy(() -> activityService.getActivityByActor(UUID.randomUUID(), 0, 10))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should return empty page when no activity")
        void getActivityByActor_empty() {
            PaginatedResponse<ActivityEventResponse> result = activityService
                    .getActivityByActor(actor.getId(), 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Get Activity Feed")
    class GetActivityFeed {

        @Test
        @DisplayName("should return paginated global activity feed")
        void getActivityFeed_paginated() {
            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", UUID.randomUUID(), null, null, null);
            activityService.logActivity(actor.getId(), "SUBMITTED", "FEEDBACK", UUID.randomUUID(), null, null, null);

            PaginatedResponse<ActivityEventResponse> result = activityService.getActivityFeed(0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("should respect page size")
        void getActivityFeed_respectsPageSize() {
            for (int i = 0; i < 5; i++) {
                activityService.logActivity(actor.getId(), "ACTION_" + i, "ENTITY", UUID.randomUUID(), null, null, null);
            }

            PaginatedResponse<ActivityEventResponse> result = activityService.getActivityFeed(0, 3);

            assertThat(result.getContent()).hasSizeLessThanOrEqualTo(3);
            assertThat(result.getSize()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Get Filtered Activity")
    class GetFilteredActivity {

        @Test
        @DisplayName("should filter by entity type")
        void getFilteredActivity_byEntityType() {
            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", UUID.randomUUID(), null, null, null);
            activityService.logActivity(actor.getId(), "CREATED", "CANDIDATE", UUID.randomUUID(), null, null, null);

            ActivityFilterRequest filter = ActivityFilterRequest.builder()
                    .entityType("INTERVIEW")
                    .build();

            PaginatedResponse<ActivityEventResponse> result = activityService
                    .getFilteredActivity(filter, 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).allMatch(r -> r.getEntityType().equals("INTERVIEW"));
        }

        @Test
        @DisplayName("should filter by action")
        void getFilteredActivity_byAction() {
            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", UUID.randomUUID(), null, null, null);
            activityService.logActivity(actor.getId(), "UPDATED", "INTERVIEW", UUID.randomUUID(), null, null, null);

            ActivityFilterRequest filter = ActivityFilterRequest.builder()
                    .action("CREATED")
                    .build();

            PaginatedResponse<ActivityEventResponse> result = activityService
                    .getFilteredActivity(filter, 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).allMatch(r -> r.getAction().equals("CREATED"));
        }

        @Test
        @DisplayName("should filter by date range")
        void getFilteredActivity_byDateRange() {
            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", UUID.randomUUID(), null, null, null);

            Instant startDate = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant endDate = Instant.now().plus(1, ChronoUnit.HOURS);

            ActivityFilterRequest filter = ActivityFilterRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            PaginatedResponse<ActivityEventResponse> result = activityService
                    .getFilteredActivity(filter, 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent()).allMatch(r ->
                    !r.getCreatedAt().isBefore(startDate) && !r.getCreatedAt().isAfter(endDate));
        }

        @Test
        @DisplayName("should filter by actor id")
        void getFilteredActivity_byActorId() {
            User otherActor = userRepository.save(User.builder()
                    .firstName("Other")
                    .lastName("User")
                    .email("other-" + UUID.randomUUID() + "@test.com")
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build());

            activityService.logActivity(actor.getId(), "CREATED", "INTERVIEW", UUID.randomUUID(), null, null, null);
            activityService.logActivity(otherActor.getId(), "CREATED", "INTERVIEW", UUID.randomUUID(), null, null, null);

            ActivityFilterRequest filter = ActivityFilterRequest.builder()
                    .actorId(actor.getId())
                    .build();

            PaginatedResponse<ActivityEventResponse> result = activityService
                    .getFilteredActivity(filter, 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).allMatch(r -> r.getActorId().equals(actor.getId()));
        }

        @Test
        @DisplayName("should return empty when no matches")
        void getFilteredActivity_noMatches() {
            ActivityFilterRequest filter = ActivityFilterRequest.builder()
                    .entityType("NONEXISTENT")
                    .build();

            PaginatedResponse<ActivityEventResponse> result = activityService
                    .getFilteredActivity(filter, 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("logInterviewCreated should create proper event")
        void logInterviewCreated_success() {
            UUID interviewId = UUID.randomUUID();

            activityService.logInterviewCreated(actor.getId(), interviewId, "Technical Interview");

            List<ActivityEventResponse> events = activityService.getActivityForEntity("INTERVIEW", interviewId);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getAction()).isEqualTo("CREATED");
            assertThat(events.get(0).getEntityType()).isEqualTo("INTERVIEW");
            assertThat(events.get(0).getEntityId()).isEqualTo(interviewId);
            assertThat(events.get(0).getMetadata()).containsEntry("title", "Technical Interview");
        }

        @Test
        @DisplayName("logInterviewScheduled should create proper event")
        void logInterviewScheduled_success() {
            UUID interviewId = UUID.randomUUID();

            activityService.logInterviewScheduled(actor.getId(), interviewId, "Backend Interview");

            List<ActivityEventResponse> events = activityService.getActivityForEntity("INTERVIEW", interviewId);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getAction()).isEqualTo("SCHEDULED");
            assertThat(events.get(0).getEntityType()).isEqualTo("INTERVIEW");
            assertThat(events.get(0).getMetadata()).containsEntry("title", "Backend Interview");
        }

        @Test
        @DisplayName("logFeedbackSubmitted should create proper event")
        void logFeedbackSubmitted_success() {
            UUID feedbackId = UUID.randomUUID();
            UUID interviewId = UUID.randomUUID();

            activityService.logFeedbackSubmitted(actor.getId(), feedbackId, interviewId);

            List<ActivityEventResponse> events = activityService.getActivityForEntity("FEEDBACK", feedbackId);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getAction()).isEqualTo("SUBMITTED");
            assertThat(events.get(0).getEntityType()).isEqualTo("FEEDBACK");
            assertThat(events.get(0).getEntityId()).isEqualTo(feedbackId);
            assertThat(events.get(0).getTargetType()).isEqualTo("INTERVIEW");
            assertThat(events.get(0).getTargetId()).isEqualTo(interviewId);
        }

        @Test
        @DisplayName("logCandidateAdvanced should create proper event")
        void logCandidateAdvanced_success() {
            UUID candidateId = UUID.randomUUID();

            activityService.logCandidateAdvanced(actor.getId(), candidateId, "TECHNICAL_ROUND");

            List<ActivityEventResponse> events = activityService.getActivityForEntity("CANDIDATE", candidateId);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getAction()).isEqualTo("ADVANCED");
            assertThat(events.get(0).getEntityType()).isEqualTo("CANDIDATE");
            assertThat(events.get(0).getEntityId()).isEqualTo(candidateId);
            assertThat(events.get(0).getMetadata()).containsEntry("stage", "TECHNICAL_ROUND");
        }
    }
}

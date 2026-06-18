package com.interview_platform_backend.interview_platform_backend.activity.controller;

import com.interview_platform_backend.interview_platform_backend.activity.dto.ActivityEventResponse;
import com.interview_platform_backend.interview_platform_backend.activity.service.ActivityService;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityControllerWebMvcTest {

    private MockMvc mockMvc;
    private ActivityService activityService;
    private SecurityHelper securityHelper;

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID ENTITY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        activityService = mock(ActivityService.class);
        securityHelper = mock(SecurityHelper.class);
        ActivityController controller = new ActivityController(activityService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        given(securityHelper.getCurrentUserId()).willReturn(CURRENT_USER_ID);
    }

    private ActivityEventResponse sampleEvent() {
        return ActivityEventResponse.builder()
                .id(EVENT_ID)
                .actorId(CURRENT_USER_ID)
                .actorName("John Doe")
                .actorEmail("john@test.com")
                .action("CREATED")
                .entityType("INTERVIEW")
                .entityId(ENTITY_ID)
                .targetType(null)
                .targetId(null)
                .metadata(Map.of("title", "Technical Interview"))
                .createdAt(Instant.now())
                .build();
    }

    private PaginatedResponse<ActivityEventResponse> paginatedResponse(List<ActivityEventResponse> content) {
        return PaginatedResponse.<ActivityEventResponse>builder()
                .content(content)
                .page(0)
                .size(10)
                .totalElements((long) content.size())
                .totalPages(1)
                .last(true)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/activities")
    class GetActivityFeed {

        @Test
        @DisplayName("should return global activity feed")
        void getActivityFeed_success() throws Exception {
            given(activityService.getActivityFeed(0, 10))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            mockMvc.perform(get("/api/v1/activities")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(EVENT_ID.toString()))
                    .andExpect(jsonPath("$.content[0].action").value("CREATED"))
                    .andExpect(jsonPath("$.content[0].entityType").value("INTERVIEW"))
                    .andExpect(jsonPath("$.content[0].actorName").value("John Doe"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("should return empty feed")
        void getActivityFeed_empty() throws Exception {
            given(activityService.getActivityFeed(0, 10))
                    .willReturn(paginatedResponse(List.of()));

            mockMvc.perform(get("/api/v1/activities")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("should use default page and size")
        void getActivityFeed_defaultParams() throws Exception {
            given(activityService.getActivityFeed(0, 10))
                    .willReturn(paginatedResponse(List.of()));

            mockMvc.perform(get("/api/v1/activities"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/entity/{entityType}/{entityId}")
    class GetActivityForEntity {

        @Test
        @DisplayName("should return entity timeline")
        void getActivityForEntity_success() throws Exception {
            given(activityService.getActivityForEntity("INTERVIEW", ENTITY_ID))
                    .willReturn(List.of(sampleEvent()));

            mockMvc.perform(get("/api/v1/activities/entity/{entityType}/{entityId}", "INTERVIEW", ENTITY_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(EVENT_ID.toString()))
                    .andExpect(jsonPath("$[0].entityType").value("INTERVIEW"))
                    .andExpect(jsonPath("$[0].entityId").value(ENTITY_ID.toString()))
                    .andExpect(jsonPath("$[0].action").value("CREATED"));
        }

        @Test
        @DisplayName("should return empty list when no events")
        void getActivityForEntity_empty() throws Exception {
            UUID unknownId = UUID.randomUUID();
            given(activityService.getActivityForEntity("INTERVIEW", unknownId))
                    .willReturn(List.of());

            mockMvc.perform(get("/api/v1/activities/entity/{entityType}/{entityId}", "INTERVIEW", unknownId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/user/{userId}")
    class GetActivityByUser {

        @Test
        @DisplayName("should return user activity")
        void getActivityByUser_success() throws Exception {
            UUID userId = UUID.randomUUID();
            given(activityService.getActivityByActor(userId, 0, 10))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            mockMvc.perform(get("/api/v1/activities/user/{userId}", userId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(EVENT_ID.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void getActivityByUser_notFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            given(activityService.getActivityByActor(unknownId, 0, 10))
                    .willThrow(new ResourceNotFoundException("User", "id", unknownId));

            mockMvc.perform(get("/api/v1/activities/user/{userId}", unknownId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return invalid UUID as bad request")
        void getActivityByUser_invalidUuid() throws Exception {
            mockMvc.perform(get("/api/v1/activities/user/{userId}", "not-a-uuid")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/my")
    class GetMyActivity {

        @Test
        @DisplayName("should return current user's activity")
        void getMyActivity_success() throws Exception {
            given(activityService.getActivityByActor(CURRENT_USER_ID, 0, 10))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            mockMvc.perform(get("/api/v1/activities/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(EVENT_ID.toString()))
                    .andExpect(jsonPath("$.content[0].actorId").value(CURRENT_USER_ID.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return empty when no activity")
        void getMyActivity_empty() throws Exception {
            given(activityService.getActivityByActor(CURRENT_USER_ID, 0, 10))
                    .willReturn(paginatedResponse(List.of()));

            mockMvc.perform(get("/api/v1/activities/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("should use default pagination parameters")
        void getMyActivity_defaultParams() throws Exception {
            given(activityService.getActivityByActor(CURRENT_USER_ID, 0, 10))
                    .willReturn(paginatedResponse(List.of()));

            mockMvc.perform(get("/api/v1/activities/my"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/activities/filter")
    class GetFilteredActivity {

        @Test
        @DisplayName("should return filtered activity by entity type")
        void getFilteredActivity_byEntityType() throws Exception {
            given(activityService.getFilteredActivity(any(), eq(0), eq(10)))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            String body = """
                    {
                      "entityType": "INTERVIEW"
                    }
                    """;

            mockMvc.perform(post("/api/v1/activities/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].entityType").value("INTERVIEW"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return filtered activity by action")
        void getFilteredActivity_byAction() throws Exception {
            given(activityService.getFilteredActivity(any(), eq(0), eq(10)))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            String body = """
                    {
                      "action": "CREATED"
                    }
                    """;

            mockMvc.perform(post("/api/v1/activities/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].action").value("CREATED"));
        }

        @Test
        @DisplayName("should return filtered activity by date range")
        void getFilteredActivity_byDateRange() throws Exception {
            given(activityService.getFilteredActivity(any(), eq(0), eq(10)))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant endDate = Instant.now().plus(1, ChronoUnit.DAYS);

            String body = """
                    {
                      "startDate": "%s",
                      "endDate": "%s"
                    }
                    """.formatted(startDate.toString(), endDate.toString());

            mockMvc.perform(post("/api/v1/activities/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty());
        }

        @Test
        @DisplayName("should return filtered activity by actor id")
        void getFilteredActivity_byActorId() throws Exception {
            given(activityService.getFilteredActivity(any(), eq(0), eq(10)))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            String body = """
                    {
                      "actorId": "%s"
                    }
                    """.formatted(CURRENT_USER_ID);

            mockMvc.perform(post("/api/v1/activities/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].actorId").value(CURRENT_USER_ID.toString()));
        }

        @Test
        @DisplayName("should return empty when no matches")
        void getFilteredActivity_empty() throws Exception {
            given(activityService.getFilteredActivity(any(), eq(0), eq(10)))
                    .willReturn(paginatedResponse(List.of()));

            String body = """
                    {
                      "entityType": "NONEXISTENT"
                    }
                    """;

            mockMvc.perform(post("/api/v1/activities/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("should accept empty filter body")
        void getFilteredActivity_emptyFilter() throws Exception {
            given(activityService.getFilteredActivity(any(), eq(0), eq(10)))
                    .willReturn(paginatedResponse(List.of(sampleEvent())));

            String body = "{}";

            mockMvc.perform(post("/api/v1/activities/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
    }
}

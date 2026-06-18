package com.interview_platform_backend.interview_platform_backend.tenant.controller;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.tenant.dto.OrganizationMemberResponse;
import com.interview_platform_backend.interview_platform_backend.tenant.dto.OrganizationResponse;
import com.interview_platform_backend.interview_platform_backend.tenant.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrganizationControllerWebMvcTest {

    private MockMvc mockMvc;
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = mock(OrganizationService.class);
        OrganizationController controller = new OrganizationController(organizationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private OrganizationResponse buildOrgResponse(UUID id) {
        return OrganizationResponse.builder()
                .id(id)
                .name("Test Org")
                .slug("test-org")
                .domain("test.com")
                .logoUrl("https://example.com/logo.png")
                .plan("FREE")
                .maxUsers(5)
                .isActive(true)
                .memberCount(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private OrganizationMemberResponse buildMemberResponse(UUID userId) {
        return OrganizationMemberResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .userEmail("member@example.com")
                .userName("Test Member")
                .role("MEMBER")
                .joinedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/organizations")
    class CreateOrganization {

        @Test
        @DisplayName("should create organization and return 201")
        void createOrganization_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            given(organizationService.createOrganization(any())).willReturn(buildOrgResponse(orgId));

            String body = """
                    {
                      "name": "Test Org",
                      "slug": "test-org",
                      "domain": "test.com",
                      "logoUrl": "https://example.com/logo.png",
                      "plan": "FREE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/organizations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(orgId.toString()))
                    .andExpect(jsonPath("$.name").value("Test Org"))
                    .andExpect(jsonPath("$.slug").value("test-org"))
                    .andExpect(jsonPath("$.plan").value("FREE"));

            verify(organizationService).createOrganization(any());
        }

        @Test
        @DisplayName("should return 409 for duplicate slug")
        void createOrganization_duplicateSlug() throws Exception {
            given(organizationService.createOrganization(any()))
                    .willThrow(new DuplicateResourceException("Organization", "slug", "test-org"));

            String body = """
                    {
                      "name": "Test Org",
                      "slug": "test-org"
                    }
                    """;

            mockMvc.perform(post("/api/v1/organizations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void createOrganization_missingFields() throws Exception {
            String body = """
                    {
                      "domain": "test.com"
                    }
                    """;

            mockMvc.perform(post("/api/v1/organizations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for invalid plan")
        void createOrganization_invalidPlan() throws Exception {
            given(organizationService.createOrganization(any()))
                    .willThrow(new BadRequestException("Invalid plan: BOGUS"));

            String body = """
                    {
                      "name": "Test Org",
                      "slug": "test-org",
                      "plan": "BOGUS"
                    }
                    """;

            mockMvc.perform(post("/api/v1/organizations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}")
    class GetOrganization {

        @Test
        @DisplayName("should return organization and 200")
        void getOrganization_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            given(organizationService.getOrganization(orgId)).willReturn(buildOrgResponse(orgId));

            mockMvc.perform(get("/api/v1/organizations/{id}", orgId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orgId.toString()))
                    .andExpect(jsonPath("$.name").value("Test Org"));

            verify(organizationService).getOrganization(orgId);
        }

        @Test
        @DisplayName("should return 404 for non-existent organization")
        void getOrganization_notFound() throws Exception {
            UUID orgId = UUID.randomUUID();
            given(organizationService.getOrganization(orgId))
                    .willThrow(new ResourceNotFoundException("Organization", "id", orgId));

            mockMvc.perform(get("/api/v1/organizations/{id}", orgId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/organizations/{id}")
    class UpdateOrganization {

        @Test
        @DisplayName("should update organization and return 200")
        void updateOrganization_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            OrganizationResponse response = buildOrgResponse(orgId);
            response.setName("Updated Org");
            given(organizationService.updateOrganization(eq(orgId), any())).willReturn(response);

            String body = """
                    {
                      "name": "Updated Org"
                    }
                    """;

            mockMvc.perform(put("/api/v1/organizations/{id}", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Org"));

            verify(organizationService).updateOrganization(eq(orgId), any());
        }

        @Test
        @DisplayName("should return 404 for non-existent organization")
        void updateOrganization_notFound() throws Exception {
            UUID orgId = UUID.randomUUID();
            given(organizationService.updateOrganization(eq(orgId), any()))
                    .willThrow(new ResourceNotFoundException("Organization", "id", orgId));

            String body = """
                    {
                      "name": "Updated Org"
                    }
                    """;

            mockMvc.perform(put("/api/v1/organizations/{id}", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 for duplicate slug on update")
        void updateOrganization_duplicateSlug() throws Exception {
            UUID orgId = UUID.randomUUID();
            given(organizationService.updateOrganization(eq(orgId), any()))
                    .willThrow(new DuplicateResourceException("Organization", "slug", "existing-slug"));

            String body = """
                    {
                      "slug": "existing-slug"
                    }
                    """;

            mockMvc.perform(put("/api/v1/organizations/{id}", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/organizations/{id}")
    class DeleteOrganization {

        @Test
        @DisplayName("should delete organization and return 204")
        void deleteOrganization_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            doNothing().when(organizationService).deleteOrganization(orgId);

            mockMvc.perform(delete("/api/v1/organizations/{id}", orgId))
                    .andExpect(status().isNoContent());

            verify(organizationService).deleteOrganization(orgId);
        }

        @Test
        @DisplayName("should return 404 for non-existent organization")
        void deleteOrganization_notFound() throws Exception {
            UUID orgId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Organization", "id", orgId))
                    .when(organizationService).deleteOrganization(orgId);

            mockMvc.perform(delete("/api/v1/organizations/{id}", orgId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organizations/my")
    class GetMyOrganizations {

        @Test
        @DisplayName("should return user's organizations and 200")
        void getMyOrganizations_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            given(organizationService.getOrganizationsByUser())
                    .willReturn(List.of(buildOrgResponse(orgId)));

            mockMvc.perform(get("/api/v1/organizations/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(orgId.toString()));

            verify(organizationService).getOrganizationsByUser();
        }

        @Test
        @DisplayName("should return empty list when user has no organizations")
        void getMyOrganizations_empty() throws Exception {
            given(organizationService.getOrganizationsByUser()).willReturn(List.of());

            mockMvc.perform(get("/api/v1/organizations/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/organizations/{id}/members")
    class AddMember {

        @Test
        @DisplayName("should add member and return 201")
        void addMember_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.addMember(eq(orgId), any())).willReturn(buildMemberResponse(userId));

            String body = """
                    {
                      "userId": "%s",
                      "role": "MEMBER"
                    }
                    """.formatted(userId);

            mockMvc.perform(post("/api/v1/organizations/{id}/members", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.role").value("MEMBER"));

            verify(organizationService).addMember(eq(orgId), any());
        }

        @Test
        @DisplayName("should return 404 when organization not found")
        void addMember_orgNotFound() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.addMember(eq(orgId), any()))
                    .willThrow(new ResourceNotFoundException("Organization", "id", orgId));

            String body = """
                    {
                      "userId": "%s",
                      "role": "MEMBER"
                    }
                    """.formatted(userId);

            mockMvc.perform(post("/api/v1/organizations/{id}/members", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when member already exists")
        void addMember_duplicate() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.addMember(eq(orgId), any()))
                    .willThrow(new DuplicateResourceException("OrganizationMember", "userId", userId));

            String body = """
                    {
                      "userId": "%s",
                      "role": "MEMBER"
                    }
                    """.formatted(userId);

            mockMvc.perform(post("/api/v1/organizations/{id}/members", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 for invalid role")
        void addMember_invalidRole() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.addMember(eq(orgId), any()))
                    .willThrow(new BadRequestException("Invalid role: INVALID"));

            String body = """
                    {
                      "userId": "%s",
                      "role": "INVALID"
                    }
                    """.formatted(userId);

            mockMvc.perform(post("/api/v1/organizations/{id}/members", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when max members reached")
        void addMember_maxMembersReached() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.addMember(eq(orgId), any()))
                    .willThrow(new BadRequestException("Organization has reached maximum member limit of 5"));

            String body = """
                    {
                      "userId": "%s",
                      "role": "MEMBER"
                    }
                    """.formatted(userId);

            mockMvc.perform(post("/api/v1/organizations/{id}/members", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/organizations/{id}/members/{userId}")
    class RemoveMember {

        @Test
        @DisplayName("should remove member and return 204")
        void removeMember_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            doNothing().when(organizationService).removeMember(orgId, userId);

            mockMvc.perform(delete("/api/v1/organizations/{id}/members/{userId}", orgId, userId))
                    .andExpect(status().isNoContent());

            verify(organizationService).removeMember(orgId, userId);
        }

        @Test
        @DisplayName("should return 404 when member not found")
        void removeMember_notFound() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("OrganizationMember", "userId", userId))
                    .when(organizationService).removeMember(orgId, userId);

            mockMvc.perform(delete("/api/v1/organizations/{id}/members/{userId}", orgId, userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when removing last owner")
        void removeMember_lastOwner() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            doThrow(new BadRequestException("Cannot remove the last owner of the organization"))
                    .when(organizationService).removeMember(orgId, userId);

            mockMvc.perform(delete("/api/v1/organizations/{id}/members/{userId}", orgId, userId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}/members")
    class GetMembers {

        @Test
        @DisplayName("should return members list and 200")
        void getMembers_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.getMembers(orgId))
                    .willReturn(List.of(buildMemberResponse(userId)));

            mockMvc.perform(get("/api/v1/organizations/{id}/members", orgId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                    .andExpect(jsonPath("$[0].role").value("MEMBER"));

            verify(organizationService).getMembers(orgId);
        }

        @Test
        @DisplayName("should return 404 when organization not found")
        void getMembers_orgNotFound() throws Exception {
            UUID orgId = UUID.randomUUID();
            given(organizationService.getMembers(orgId))
                    .willThrow(new ResourceNotFoundException("Organization", "id", orgId));

            mockMvc.perform(get("/api/v1/organizations/{id}/members", orgId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/organizations/{id}/members/{userId}/role")
    class UpdateMemberRole {

        @Test
        @DisplayName("should update member role and return 200")
        void updateMemberRole_success() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            OrganizationMemberResponse response = buildMemberResponse(userId);
            response.setRole("ADMIN");
            given(organizationService.updateMemberRole(orgId, userId, "ADMIN")).willReturn(response);

            mockMvc.perform(patch("/api/v1/organizations/{id}/members/{userId}/role", orgId, userId)
                            .param("role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            verify(organizationService).updateMemberRole(orgId, userId, "ADMIN");
        }

        @Test
        @DisplayName("should return 404 when member not found")
        void updateMemberRole_memberNotFound() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.updateMemberRole(orgId, userId, "ADMIN"))
                    .willThrow(new ResourceNotFoundException("OrganizationMember", "userId", userId));

            mockMvc.perform(patch("/api/v1/organizations/{id}/members/{userId}/role", orgId, userId)
                            .param("role", "ADMIN"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 for invalid role")
        void updateMemberRole_invalidRole() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.updateMemberRole(orgId, userId, "BOGUS"))
                    .willThrow(new BadRequestException("Invalid role: BOGUS"));

            mockMvc.perform(patch("/api/v1/organizations/{id}/members/{userId}/role", orgId, userId)
                            .param("role", "BOGUS"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when changing last owner role")
        void updateMemberRole_lastOwner() throws Exception {
            UUID orgId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(organizationService.updateMemberRole(orgId, userId, "MEMBER"))
                    .willThrow(new BadRequestException("Cannot change role of the last owner"));

            mockMvc.perform(patch("/api/v1/organizations/{id}/members/{userId}/role", orgId, userId)
                            .param("role", "MEMBER"))
                    .andExpect(status().isBadRequest());
        }
    }
}

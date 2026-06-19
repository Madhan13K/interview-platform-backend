package com.interview_platform_backend.interview_platform_backend.webhook.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.webhook.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class WebhookServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .firstName("Webhook")
                .lastName("Tester")
                .email("webhook-tester-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
    }

    private CreateWebhookRequest buildCreateRequest() {
        return CreateWebhookRequest.builder()
                .url("https://example.com/webhook")
                .description("Test webhook endpoint")
                .events(List.of("INTERVIEW_SCHEDULED", "FEEDBACK_SUBMITTED"))
                .build();
    }

    @Nested
    @DisplayName("Create Webhook")
    class CreateWebhook {

        @Test
        @DisplayName("should create webhook with generated secret")
        void createWebhook_success() {
            WebhookEndpointResponse response = webhookService.createWebhook(buildCreateRequest(), testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getUrl()).isEqualTo("https://example.com/webhook");
            assertThat(response.getDescription()).isEqualTo("Test webhook endpoint");
            assertThat(response.getEvents()).containsExactlyInAnyOrder("INTERVIEW_SCHEDULED", "FEEDBACK_SUBMITTED");
            assertThat(response.getIsActive()).isTrue();
            assertThat(response.getSecret()).isNotNull();
            assertThat(response.getSecret()).startsWith("whsec_");
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void createWebhook_userNotFound() {
            UUID nonExistentUserId = UUID.randomUUID();

            assertThatThrownBy(() -> webhookService.createWebhook(buildCreateRequest(), nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Webhook")
    class UpdateWebhook {

        @Test
        @DisplayName("should update webhook fields")
        void updateWebhook_success() {
            WebhookEndpointResponse created = webhookService.createWebhook(buildCreateRequest(), testUser.getId());

            UpdateWebhookRequest updateRequest = UpdateWebhookRequest.builder()
                    .url("https://updated.com/hook")
                    .description("Updated description")
                    .events(List.of("CANDIDATE_HIRED"))
                    .isActive(false)
                    .build();

            WebhookEndpointResponse updated = webhookService.updateWebhook(created.getId(), updateRequest);

            assertThat(updated.getUrl()).isEqualTo("https://updated.com/hook");
            assertThat(updated.getDescription()).isEqualTo("Updated description");
            assertThat(updated.getEvents()).containsExactly("CANDIDATE_HIRED");
            assertThat(updated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when webhook not found")
        void updateWebhook_notFound() {
            UUID nonExistentId = UUID.randomUUID();
            UpdateWebhookRequest updateRequest = UpdateWebhookRequest.builder()
                    .url("https://updated.com/hook")
                    .build();

            assertThatThrownBy(() -> webhookService.updateWebhook(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Webhook")
    class DeleteWebhook {

        @Test
        @DisplayName("should delete webhook successfully")
        void deleteWebhook_success() {
            WebhookEndpointResponse created = webhookService.createWebhook(buildCreateRequest(), testUser.getId());

            webhookService.deleteWebhook(created.getId());

            assertThatThrownBy(() -> webhookService.getWebhook(created.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when webhook not found")
        void deleteWebhook_notFound() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> webhookService.deleteWebhook(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Webhook")
    class GetWebhook {

        @Test
        @DisplayName("should get webhook with masked secret")
        void getWebhook_success() {
            WebhookEndpointResponse created = webhookService.createWebhook(buildCreateRequest(), testUser.getId());

            WebhookEndpointResponse found = webhookService.getWebhook(created.getId());

            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getUrl()).isEqualTo("https://example.com/webhook");
            assertThat(found.getSecret()).startsWith("****");
            assertThat(found.getSecret()).hasSize(8); // "****" + last 4 chars
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when webhook not found")
        void getWebhook_notFound() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> webhookService.getWebhook(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get My Webhooks")
    class GetMyWebhooks {

        @Test
        @DisplayName("should return list of user webhooks")
        void getMyWebhooks_success() {
            webhookService.createWebhook(buildCreateRequest(), testUser.getId());

            CreateWebhookRequest secondRequest = CreateWebhookRequest.builder()
                    .url("https://example.com/webhook2")
                    .description("Second webhook")
                    .events(List.of("INTERVIEW_COMPLETED"))
                    .build();
            webhookService.createWebhook(secondRequest, testUser.getId());

            List<WebhookEndpointResponse> webhooks = webhookService.getMyWebhooks(testUser.getId());

            assertThat(webhooks).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when user has no webhooks")
        void getMyWebhooks_emptyList() {
            List<WebhookEndpointResponse> webhooks = webhookService.getMyWebhooks(testUser.getId());

            assertThat(webhooks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Regenerate Secret")
    class RegenerateSecret {

        @Test
        @DisplayName("should generate a new secret different from old one")
        void regenerateSecret_newSecretDifferent() {
            WebhookEndpointResponse created = webhookService.createWebhook(buildCreateRequest(), testUser.getId());
            String originalSecret = created.getSecret();

            WebhookEndpointResponse regenerated = webhookService.regenerateSecret(created.getId());

            assertThat(regenerated.getSecret()).isNotNull();
            assertThat(regenerated.getSecret()).startsWith("whsec_");
            assertThat(regenerated.getSecret()).isNotEqualTo(originalSecret);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when webhook not found")
        void regenerateSecret_notFound() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> webhookService.regenerateSecret(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Deliveries")
    class GetDeliveries {

        @Test
        @DisplayName("should return paginated deliveries (may be empty)")
        void getDeliveries_paginated() {
            WebhookEndpointResponse created = webhookService.createWebhook(buildCreateRequest(), testUser.getId());

            PaginatedResponse<WebhookDeliveryResponse> deliveries =
                    webhookService.getDeliveries(created.getId(), 0, 10);

            assertThat(deliveries).isNotNull();
            assertThat(deliveries.getContent()).isNotNull();
            assertThat(deliveries.getPage()).isEqualTo(0);
            assertThat(deliveries.getSize()).isEqualTo(10);
            assertThat(deliveries.getTotalElements()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when endpoint not found")
        void getDeliveries_endpointNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> webhookService.getDeliveries(nonExistentId, 0, 10))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

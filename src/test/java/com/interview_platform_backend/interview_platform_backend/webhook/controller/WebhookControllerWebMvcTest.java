package com.interview_platform_backend.interview_platform_backend.webhook.controller;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.GlobalExceptionHandler;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.webhook.dto.*;
import com.interview_platform_backend.interview_platform_backend.webhook.entity.WebhookDelivery;
import com.interview_platform_backend.interview_platform_backend.webhook.service.WebhookService;
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

class WebhookControllerWebMvcTest {

    private MockMvc mockMvc;
    private WebhookService webhookService;
    private SecurityHelper securityHelper;

    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        webhookService = mock(WebhookService.class);
        securityHelper = mock(SecurityHelper.class);
        WebhookController controller = new WebhookController(webhookService, securityHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        currentUserId = UUID.randomUUID();
        given(securityHelper.getCurrentUserId()).willReturn(currentUserId);
    }

    private WebhookEndpointResponse buildEndpointResponse(UUID id) {
        return WebhookEndpointResponse.builder()
                .id(id)
                .url("https://example.com/webhook")
                .description("Test webhook")
                .events(List.of("INTERVIEW_SCHEDULED", "FEEDBACK_SUBMITTED"))
                .isActive(true)
                .secret("****ab12")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/webhooks")
    class CreateWebhookEndpoint {

        @Test
        @DisplayName("should create webhook and return 200")
        void createWebhook_returnsOk() throws Exception {
            UUID webhookId = UUID.randomUUID();
            given(webhookService.createWebhook(any(CreateWebhookRequest.class), eq(currentUserId)))
                    .willReturn(buildEndpointResponse(webhookId));

            String body = """
                    {
                      "url": "https://example.com/webhook",
                      "description": "Test webhook",
                      "events": ["INTERVIEW_SCHEDULED", "FEEDBACK_SUBMITTED"]
                    }
                    """;

            mockMvc.perform(post("/api/v1/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(webhookId.toString()))
                    .andExpect(jsonPath("$.url").value("https://example.com/webhook"))
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.isActive").value(true));

            verify(webhookService).createWebhook(any(CreateWebhookRequest.class), eq(currentUserId));
        }

        @Test
        @DisplayName("should return 400 when url is blank")
        void createWebhook_blankUrl_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "url": "",
                      "events": ["INTERVIEW_SCHEDULED"]
                    }
                    """;

            mockMvc.perform(post("/api/v1/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when events is empty")
        void createWebhook_emptyEvents_returnsBadRequest() throws Exception {
            String body = """
                    {
                      "url": "https://example.com/webhook",
                      "events": []
                    }
                    """;

            mockMvc.perform(post("/api/v1/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/webhooks")
    class GetMyWebhooksEndpoint {

        @Test
        @DisplayName("should return list of webhooks")
        void getMyWebhooks_returnsOk() throws Exception {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            given(webhookService.getMyWebhooks(currentUserId))
                    .willReturn(List.of(buildEndpointResponse(id1), buildEndpointResponse(id2)));

            mockMvc.perform(get("/api/v1/webhooks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));

            verify(webhookService).getMyWebhooks(currentUserId);
        }

        @Test
        @DisplayName("should return empty list when no webhooks")
        void getMyWebhooks_empty_returnsOk() throws Exception {
            given(webhookService.getMyWebhooks(currentUserId)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/webhooks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/webhooks/{id}")
    class GetWebhookEndpoint {

        @Test
        @DisplayName("should return webhook details")
        void getWebhook_returnsOk() throws Exception {
            UUID webhookId = UUID.randomUUID();
            given(webhookService.getWebhook(webhookId)).willReturn(buildEndpointResponse(webhookId));

            mockMvc.perform(get("/api/v1/webhooks/{id}", webhookId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(webhookId.toString()))
                    .andExpect(jsonPath("$.secret").value("****ab12"));

            verify(webhookService).getWebhook(webhookId);
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void getWebhook_notFound_returns404() throws Exception {
            UUID webhookId = UUID.randomUUID();
            given(webhookService.getWebhook(webhookId))
                    .willThrow(new ResourceNotFoundException("WebhookEndpoint", "id", webhookId));

            mockMvc.perform(get("/api/v1/webhooks/{id}", webhookId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/webhooks/{id}")
    class UpdateWebhookEndpoint {

        @Test
        @DisplayName("should update webhook and return 200")
        void updateWebhook_returnsOk() throws Exception {
            UUID webhookId = UUID.randomUUID();
            WebhookEndpointResponse updated = buildEndpointResponse(webhookId);
            updated.setUrl("https://updated.com/hook");
            given(webhookService.updateWebhook(eq(webhookId), any(UpdateWebhookRequest.class)))
                    .willReturn(updated);

            String body = """
                    {
                      "url": "https://updated.com/hook",
                      "isActive": false
                    }
                    """;

            mockMvc.perform(put("/api/v1/webhooks/{id}", webhookId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://updated.com/hook"));

            verify(webhookService).updateWebhook(eq(webhookId), any(UpdateWebhookRequest.class));
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void updateWebhook_notFound_returns404() throws Exception {
            UUID webhookId = UUID.randomUUID();
            given(webhookService.updateWebhook(eq(webhookId), any(UpdateWebhookRequest.class)))
                    .willThrow(new ResourceNotFoundException("WebhookEndpoint", "id", webhookId));

            String body = """
                    {
                      "url": "https://updated.com/hook"
                    }
                    """;

            mockMvc.perform(put("/api/v1/webhooks/{id}", webhookId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/webhooks/{id}")
    class DeleteWebhookEndpoint {

        @Test
        @DisplayName("should delete webhook and return 204")
        void deleteWebhook_returnsNoContent() throws Exception {
            UUID webhookId = UUID.randomUUID();
            doNothing().when(webhookService).deleteWebhook(webhookId);

            mockMvc.perform(delete("/api/v1/webhooks/{id}", webhookId))
                    .andExpect(status().isNoContent());

            verify(webhookService).deleteWebhook(webhookId);
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void deleteWebhook_notFound_returns404() throws Exception {
            UUID webhookId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("WebhookEndpoint", "id", webhookId))
                    .when(webhookService).deleteWebhook(webhookId);

            mockMvc.perform(delete("/api/v1/webhooks/{id}", webhookId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/webhooks/{id}/regenerate-secret")
    class RegenerateSecretEndpoint {

        @Test
        @DisplayName("should regenerate secret and return 200")
        void regenerateSecret_returnsOk() throws Exception {
            UUID webhookId = UUID.randomUUID();
            WebhookEndpointResponse response = buildEndpointResponse(webhookId);
            response.setSecret("whsec_newgeneratedsecret1234");
            given(webhookService.regenerateSecret(webhookId)).willReturn(response);

            mockMvc.perform(post("/api/v1/webhooks/{id}/regenerate-secret", webhookId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.secret").value("whsec_newgeneratedsecret1234"));

            verify(webhookService).regenerateSecret(webhookId);
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void regenerateSecret_notFound_returns404() throws Exception {
            UUID webhookId = UUID.randomUUID();
            given(webhookService.regenerateSecret(webhookId))
                    .willThrow(new ResourceNotFoundException("WebhookEndpoint", "id", webhookId));

            mockMvc.perform(post("/api/v1/webhooks/{id}/regenerate-secret", webhookId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/webhooks/{id}/deliveries")
    class GetDeliveriesEndpoint {

        @Test
        @DisplayName("should return paginated deliveries")
        void getDeliveries_returnsOk() throws Exception {
            UUID webhookId = UUID.randomUUID();
            PaginatedResponse<WebhookDeliveryResponse> paginatedResponse = PaginatedResponse.<WebhookDeliveryResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(10)
                    .totalElements(0L)
                    .totalPages(0)
                    .last(true)
                    .build();

            given(webhookService.getDeliveries(webhookId, 0, 10)).willReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/webhooks/{id}/deliveries", webhookId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.content").isArray());

            verify(webhookService).getDeliveries(webhookId, 0, 10);
        }

        @Test
        @DisplayName("should return 404 when endpoint not found")
        void getDeliveries_notFound_returns404() throws Exception {
            UUID webhookId = UUID.randomUUID();
            given(webhookService.getDeliveries(webhookId, 0, 10))
                    .willThrow(new ResourceNotFoundException("WebhookEndpoint", "id", webhookId));

            mockMvc.perform(get("/api/v1/webhooks/{id}/deliveries", webhookId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/webhooks/deliveries/{deliveryId}/retry")
    class RetryDeliveryEndpoint {

        @Test
        @DisplayName("should retry delivery and return 200")
        void retryDelivery_returnsOk() throws Exception {
            UUID deliveryId = UUID.randomUUID();
            UUID endpointId = UUID.randomUUID();
            WebhookDeliveryResponse deliveryResponse = WebhookDeliveryResponse.builder()
                    .id(deliveryId)
                    .endpointId(endpointId)
                    .eventType("INTERVIEW_SCHEDULED")
                    .payload("{\"interview_id\": \"123\"}")
                    .attempt(0)
                    .maxAttempts(5)
                    .status(WebhookDelivery.DeliveryStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();

            given(webhookService.retryDelivery(deliveryId)).willReturn(deliveryResponse);

            mockMvc.perform(post("/api/v1/webhooks/deliveries/{deliveryId}/retry", deliveryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(deliveryId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(webhookService).retryDelivery(deliveryId);
        }

        @Test
        @DisplayName("should return 404 when delivery not found")
        void retryDelivery_notFound_returns404() throws Exception {
            UUID deliveryId = UUID.randomUUID();
            given(webhookService.retryDelivery(deliveryId))
                    .willThrow(new ResourceNotFoundException("WebhookDelivery", "id", deliveryId));

            mockMvc.perform(post("/api/v1/webhooks/deliveries/{deliveryId}/retry", deliveryId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when delivery cannot be retried")
        void retryDelivery_cannotRetry_returnsBadRequest() throws Exception {
            UUID deliveryId = UUID.randomUUID();
            given(webhookService.retryDelivery(deliveryId))
                    .willThrow(new BadRequestException("Only FAILED or RETRYING deliveries can be retried"));

            mockMvc.perform(post("/api/v1/webhooks/deliveries/{deliveryId}/retry", deliveryId))
                    .andExpect(status().isBadRequest());
        }
    }
}

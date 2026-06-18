package com.interview_platform_backend.interview_platform_backend.webhook.controller;

import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.webhook.dto.*;
import com.interview_platform_backend.interview_platform_backend.webhook.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Webhook integration management endpoints")
public class WebhookController {

    private final WebhookService webhookService;
    private final SecurityHelper securityHelper;

    public WebhookController(WebhookService webhookService, SecurityHelper securityHelper) {
        this.webhookService = webhookService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Create a new webhook endpoint")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<WebhookEndpointResponse> createWebhook(
            @RequestBody @Valid CreateWebhookRequest request) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(webhookService.createWebhook(request, currentUserId));
    }

    @Operation(summary = "Get all webhooks for current user")
    @ApiResponse(responseCode = "200", description = "List of webhooks")
    @GetMapping
    public ResponseEntity<List<WebhookEndpointResponse>> getMyWebhooks() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(webhookService.getMyWebhooks(currentUserId));
    }

    @Operation(summary = "Get webhook details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook details"),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<WebhookEndpointResponse> getWebhook(@PathVariable UUID id) {
        return ResponseEntity.ok(webhookService.getWebhook(id));
    }

    @Operation(summary = "Update a webhook endpoint")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook updated"),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<WebhookEndpointResponse> updateWebhook(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateWebhookRequest request) {
        return ResponseEntity.ok(webhookService.updateWebhook(id, request));
    }

    @Operation(summary = "Delete a webhook endpoint")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Webhook deleted"),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable UUID id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Regenerate webhook secret")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secret regenerated"),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    @PostMapping("/{id}/regenerate-secret")
    public ResponseEntity<WebhookEndpointResponse> regenerateSecret(@PathVariable UUID id) {
        return ResponseEntity.ok(webhookService.regenerateSecret(id));
    }

    @Operation(summary = "Get deliveries for a webhook endpoint")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of deliveries"),
            @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    @GetMapping("/{id}/deliveries")
    public ResponseEntity<PaginatedResponse<WebhookDeliveryResponse>> getDeliveries(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(webhookService.getDeliveries(id, page, size));
    }

    @Operation(summary = "Retry a failed webhook delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery queued for retry"),
            @ApiResponse(responseCode = "400", description = "Delivery cannot be retried"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    @PostMapping("/deliveries/{deliveryId}/retry")
    public ResponseEntity<WebhookDeliveryResponse> retryDelivery(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(webhookService.retryDelivery(deliveryId));
    }
}

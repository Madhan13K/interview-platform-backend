package com.interview_platform_backend.interview_platform_backend.security.apikey.controller;

import com.interview_platform_backend.interview_platform_backend.security.apikey.dto.ApiKeyResponse;
import com.interview_platform_backend.interview_platform_backend.security.apikey.dto.CreateApiKeyRequest;
import com.interview_platform_backend.interview_platform_backend.security.apikey.service.ApiKeyService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final SecurityHelper securityHelper;

    public ApiKeyController(ApiKeyService apiKeyService, SecurityHelper securityHelper) {
        this.apiKeyService = apiKeyService;
        this.securityHelper = securityHelper;
    }

    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        ApiKeyResponse response = apiKeyService.createApiKey(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
        UUID userId = securityHelper.getCurrentUserId();
        List<ApiKeyResponse> keys = apiKeyService.listApiKeys(userId);
        return ResponseEntity.ok(keys);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID id) {
        UUID userId = securityHelper.getCurrentUserId();
        apiKeyService.revokeApiKey(id, userId);
        return ResponseEntity.noContent().build();
    }
}

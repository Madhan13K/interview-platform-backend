package com.interview_platform_backend.interview_platform_backend.security.apikey.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.apikey.dto.ApiKeyResponse;
import com.interview_platform_backend.interview_platform_backend.security.apikey.dto.CreateApiKeyRequest;
import com.interview_platform_backend.interview_platform_backend.security.apikey.entity.ApiKey;
import com.interview_platform_backend.interview_platform_backend.security.apikey.repository.ApiKeyRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApiKeyService {

    private static final String KEY_PREFIX = "ipk_";
    private static final int KEY_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    public ApiKeyResponse createApiKey(CreateApiKeyRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Generate random key
        String randomPart = generateRandomString(KEY_LENGTH);
        String fullKey = KEY_PREFIX + randomPart;

        // Hash the key
        String keyHash = hashKey(fullKey);

        // Store prefix (first 8 chars after ipk_)
        String keyPrefixValue = randomPart.substring(0, 8);

        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .name(request.getName())
                .keyHash(keyHash)
                .keyPrefix(keyPrefixValue)
                .scopes(request.getScopes().toArray(new String[0]))
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .createdAt(Instant.now())
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);

        // Return response with full key (only time it's visible)
        return mapToResponse(saved, fullKey);
    }

    public ApiKey validateApiKey(String rawKey) {
        String keyHash = hashKey(rawKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash).orElse(null);

        if (apiKey == null) {
            return null;
        }

        if (!apiKey.getIsActive()) {
            return null;
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }

        // Update last used timestamp
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        return apiKey;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID userId) {
        return apiKeyRepository.findByUserId(userId).stream()
                .map(key -> mapToResponse(key, null))
                .collect(Collectors.toList());
    }

    public void revokeApiKey(UUID keyId, UUID userId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", keyId));

        if (!apiKey.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only revoke your own API keys");
        }

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey, String fullKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .scopes(Arrays.asList(apiKey.getScopes()))
                .isActive(apiKey.getIsActive())
                .lastUsedAt(apiKey.getLastUsedAt())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .fullKey(fullKey)
                .build();
    }
}

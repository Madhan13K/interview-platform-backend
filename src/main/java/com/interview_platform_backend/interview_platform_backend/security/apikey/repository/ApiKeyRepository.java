package com.interview_platform_backend.interview_platform_backend.security.apikey.repository;

import com.interview_platform_backend.interview_platform_backend.security.apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUserId(UUID userId);

    List<ApiKey> findByIsActiveTrue();
}

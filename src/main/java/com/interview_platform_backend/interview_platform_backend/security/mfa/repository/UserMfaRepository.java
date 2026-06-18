package com.interview_platform_backend.interview_platform_backend.security.mfa.repository;

import com.interview_platform_backend.interview_platform_backend.security.mfa.entity.UserMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMfaRepository extends JpaRepository<UserMfa, UUID> {

    Optional<UserMfa> findByUserId(UUID userId);

    boolean existsByUserIdAndIsEnabledTrue(UUID userId);
}

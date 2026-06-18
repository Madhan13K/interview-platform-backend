package com.interview_platform_backend.interview_platform_backend.gdpr.repository;

import com.interview_platform_backend.interview_platform_backend.gdpr.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    List<UserConsent> findByUserId(UUID userId);

    Optional<UserConsent> findByUserIdAndConsentType(UUID userId, String consentType);

    boolean existsByUserIdAndConsentTypeAndGrantedTrue(UUID userId, String consentType);
}

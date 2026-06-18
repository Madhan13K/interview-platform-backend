package com.interview_platform_backend.interview_platform_backend.user.repository;

import com.interview_platform_backend.interview_platform_backend.user.entity.EmailVerificationToken;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUser(User user);

    void deleteByExpiryTimeBefore(Instant time);
}


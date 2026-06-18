package com.interview_platform_backend.interview_platform_backend.user.repository;

import com.interview_platform_backend.interview_platform_backend.user.entity.PasswordResetToken;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUser(User user);

    void deleteByExpiryTimeBefore(Instant now);
}


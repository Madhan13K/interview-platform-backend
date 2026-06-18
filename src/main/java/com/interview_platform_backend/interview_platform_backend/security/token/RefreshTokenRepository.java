package com.interview_platform_backend.interview_platform_backend.security.token;

import com.interview_platform_backend.interview_platform_backend.security.token.RefreshToken;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    void deleteByUser(User user);
    long deleteByExpiryDate(Instant now);

    /**
     * Revoke all tokens in a family (for replay detection).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenFamily = :family AND rt.revoked = false")
    int revokeAllByTokenFamily(@Param("family") String family);
}

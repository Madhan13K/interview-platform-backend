package com.interview_platform_backend.interview_platform_backend.security.token;

import com.interview_platform_backend.interview_platform_backend.security.token.RefreshToken;
import com.interview_platform_backend.interview_platform_backend.security.token.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token with a new token family (for first-time login/register).
     */
    @Transactional
    public void create(User user, String token) {
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtRefreshExpiration));
        refreshToken.setRevoked(false);
        refreshToken.setTokenFamily(UUID.randomUUID().toString());
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Creates a rotated refresh token inheriting the same token family.
     * This enables replay detection — if a revoked token in this family is reused,
     * the entire family gets invalidated.
     */
    @Transactional
    public RefreshToken createRotated(User user, String newToken, String tokenFamily) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(newToken);
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtRefreshExpiration));
        refreshToken.setRevoked(false);
        refreshToken.setTokenFamily(tokenFamily);
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Revokes ALL tokens in the same family. Called when a reuse of a
     * previously-revoked token is detected (potential token theft).
     */
    @Transactional
    public void revokeTokenFamily(String tokenFamily) {
        refreshTokenRepository.revokeAllByTokenFamily(tokenFamily);
    }

    public boolean isUsable(RefreshToken refreshToken) {
        return refreshToken != null && !refreshToken.isRevoked() && refreshToken.getExpiryDate().isAfter(Instant.now());
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

}

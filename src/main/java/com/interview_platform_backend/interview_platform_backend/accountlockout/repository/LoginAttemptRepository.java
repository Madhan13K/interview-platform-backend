package com.interview_platform_backend.interview_platform_backend.accountlockout.repository;

import com.interview_platform_backend.interview_platform_backend.accountlockout.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    List<LoginAttempt> findByEmailOrderByAttemptedAtDesc(String email);

    List<LoginAttempt> findByIpAddressOrderByAttemptedAtDesc(String ipAddress);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.successful = false AND la.attemptedAt > :since")
    long countFailedAttemptsSince(@Param("email") String email, @Param("since") Instant since);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.successful = false AND la.attemptedAt > :since")
    long countFailedAttemptsFromIpSince(@Param("ipAddress") String ipAddress, @Param("since") Instant since);

    @Query("SELECT la FROM LoginAttempt la WHERE la.email = :email AND la.successful = false AND la.attemptedAt > :since ORDER BY la.attemptedAt DESC")
    List<LoginAttempt> findRecentFailedAttempts(@Param("email") String email, @Param("since") Instant since);

    void deleteByAttemptedAtBefore(Instant before);
}

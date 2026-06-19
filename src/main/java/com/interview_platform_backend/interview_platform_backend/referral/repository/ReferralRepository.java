package com.interview_platform_backend.interview_platform_backend.referral.repository;

import com.interview_platform_backend.interview_platform_backend.referral.entity.Referral;
import com.interview_platform_backend.interview_platform_backend.referral.entity.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    List<Referral> findByReferrerIdOrderByCreatedAtDesc(UUID referrerId);

    Optional<Referral> findByReferralCode(String referralCode);

    List<Referral> findByCandidateEmail(String candidateEmail);

    long countByReferrerIdAndStatus(UUID referrerId, ReferralStatus status);

    long countByReferrerId(UUID referrerId);

    boolean existsByReferrerIdAndCandidateEmail(UUID referrerId, String candidateEmail);

    @Query("SELECT r.referrer.id, COUNT(r) as cnt FROM Referral r WHERE r.status = :status GROUP BY r.referrer.id ORDER BY cnt DESC")
    List<Object[]> findTopReferrersByStatus(@Param("status") ReferralStatus status);

    @Query("SELECT COALESCE(SUM(r.bonusAmount), 0) FROM Referral r WHERE r.referrer.id = :referrerId AND r.bonusPaidAt IS NOT NULL")
    java.math.BigDecimal sumBonusPaidByReferrerId(@Param("referrerId") UUID referrerId);

    @Query("SELECT r.referrer.id, r.referrer.firstName, r.referrer.lastName, r.referrer.email, COUNT(r) as cnt " +
            "FROM Referral r WHERE r.status = 'HIRED' GROUP BY r.referrer.id, r.referrer.firstName, r.referrer.lastName, r.referrer.email ORDER BY cnt DESC")
    List<Object[]> findLeaderboard();
}

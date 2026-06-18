package com.interview_platform_backend.interview_platform_backend.offer.repository;

import com.interview_platform_backend.interview_platform_backend.offer.entity.OfferLetter;
import com.interview_platform_backend.interview_platform_backend.offer.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfferLetterRepository extends JpaRepository<OfferLetter, UUID> {

    List<OfferLetter> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<OfferLetter> findByJobPositionIdOrderByCreatedAtDesc(UUID jobPositionId);

    List<OfferLetter> findByStatus(OfferStatus status);

    @Query("SELECT o FROM OfferLetter o WHERE o.candidate.email = :email ORDER BY o.createdAt DESC")
    List<OfferLetter> findByCandidateEmail(@Param("email") String email);

    @Query("SELECT o FROM OfferLetter o WHERE o.candidate.id = :candidateId AND o.jobPosition.id = :jobPositionId AND o.status NOT IN ('DECLINED', 'EXPIRED', 'REVOKED')")
    List<OfferLetter> findActiveOfferForCandidateAndPosition(
            @Param("candidateId") UUID candidateId,
            @Param("jobPositionId") UUID jobPositionId);
}

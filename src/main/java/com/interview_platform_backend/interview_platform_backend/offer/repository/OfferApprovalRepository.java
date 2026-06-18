package com.interview_platform_backend.interview_platform_backend.offer.repository;

import com.interview_platform_backend.interview_platform_backend.offer.entity.ApprovalStatus;
import com.interview_platform_backend.interview_platform_backend.offer.entity.OfferApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferApprovalRepository extends JpaRepository<OfferApproval, UUID> {

    List<OfferApproval> findByOfferLetterIdOrderByApprovalOrderAsc(UUID offerLetterId);

    Optional<OfferApproval> findByOfferLetterIdAndApproverEmail(UUID offerLetterId, String approverEmail);

    Optional<OfferApproval> findByOfferLetterIdAndApproverIdAndStatus(
            UUID offerLetterId, UUID approverId, ApprovalStatus status);

    List<OfferApproval> findByApproverIdAndStatus(UUID approverId, ApprovalStatus status);
}

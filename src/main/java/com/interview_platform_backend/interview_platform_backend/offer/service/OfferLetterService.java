package com.interview_platform_backend.interview_platform_backend.offer.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.jobposition.repository.JobPositionRepository;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.offer.dto.*;
import com.interview_platform_backend.interview_platform_backend.offer.entity.*;
import com.interview_platform_backend.interview_platform_backend.offer.esignature.ESignatureService;
import com.interview_platform_backend.interview_platform_backend.offer.repository.OfferApprovalRepository;
import com.interview_platform_backend.interview_platform_backend.offer.repository.OfferLetterRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfferLetterService {

    private static final Logger log = LoggerFactory.getLogger(OfferLetterService.class);

    private final OfferLetterRepository offerLetterRepository;
    private final OfferApprovalRepository offerApprovalRepository;
    private final UserRepository userRepository;
    private final JobPositionRepository jobPositionRepository;
    private final EmailNotificationService emailNotificationService;
    private final ESignatureService docuSignService;
    private final ESignatureService helloSignService;

    public OfferLetterService(
            OfferLetterRepository offerLetterRepository,
            OfferApprovalRepository offerApprovalRepository,
            UserRepository userRepository,
            JobPositionRepository jobPositionRepository,
            EmailNotificationService emailNotificationService,
            @Qualifier("docuSignService") ESignatureService docuSignService,
            @Qualifier("helloSignService") ESignatureService helloSignService) {
        this.offerLetterRepository = offerLetterRepository;
        this.offerApprovalRepository = offerApprovalRepository;
        this.userRepository = userRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.emailNotificationService = emailNotificationService;
        this.docuSignService = docuSignService;
        this.helloSignService = helloSignService;
    }

    @Transactional
    public OfferLetterResponse createOffer(CreateOfferRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", creatorEmail));

        User candidate = userRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getCandidateId()));

        JobPosition jobPosition = jobPositionRepository.findById(request.getJobPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", request.getJobPositionId()));

        // Check for existing active offer
        List<OfferLetter> activeOffers = offerLetterRepository.findActiveOfferForCandidateAndPosition(
                request.getCandidateId(), request.getJobPositionId());
        if (!activeOffers.isEmpty()) {
            throw new BadRequestException("An active offer already exists for this candidate and position");
        }

        OfferLetter offerLetter = OfferLetter.builder()
                .candidate(candidate)
                .jobPosition(jobPosition)
                .createdBy(creator)
                .status(OfferStatus.DRAFT)
                .offerContent(request.getOfferContent())
                .salaryOffered(request.getSalaryOffered())
                .salaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "USD")
                .bonusAmount(request.getBonusAmount())
                .startDate(request.getStartDate())
                .expiresAt(request.getExpiresAt())
                .esignatureProvider(request.getEsignatureProvider() != null ? request.getEsignatureProvider() : ESignatureProvider.NONE)
                .build();

        offerLetter = offerLetterRepository.save(offerLetter);

        // Create approval workflow if approver IDs are provided
        if (request.getApproverIds() != null && !request.getApproverIds().isEmpty()) {
            for (int i = 0; i < request.getApproverIds().size(); i++) {
                UUID approverId = request.getApproverIds().get(i);
                User approver = userRepository.findById(approverId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", approverId));

                OfferApproval approval = OfferApproval.builder()
                        .offerLetter(offerLetter)
                        .approver(approver)
                        .status(ApprovalStatus.PENDING)
                        .approvalOrder(i + 1)
                        .build();

                offerApprovalRepository.save(approval);
                offerLetter.getApprovals().add(approval);
            }
        }

        log.info("Offer letter created: {} for candidate {} by {}", offerLetter.getId(), candidate.getEmail(), creatorEmail);
        return mapToResponse(offerLetter);
    }

    @Transactional
    public OfferLetterResponse submitForApproval(UUID offerId) {
        OfferLetter offerLetter = findOfferById(offerId);

        if (offerLetter.getStatus() != OfferStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT offers can be submitted for approval");
        }

        List<OfferApproval> approvals = offerApprovalRepository.findByOfferLetterIdOrderByApprovalOrderAsc(offerId);

        if (approvals.isEmpty()) {
            // No approvers configured, auto-approve
            offerLetter.setStatus(OfferStatus.APPROVED);
            log.info("Offer {} auto-approved (no approvers configured)", offerId);
        } else {
            offerLetter.setStatus(OfferStatus.PENDING_APPROVAL);
            // Notify the first approver
            OfferApproval firstApproval = approvals.get(0);
            firstApproval.setRequestedAt(Instant.now());
            offerApprovalRepository.save(firstApproval);

            emailNotificationService.sendEmail(
                    firstApproval.getApprover().getEmail(),
                    "Offer Letter Pending Your Approval",
                    String.format("An offer letter for %s %s (position: %s) requires your approval.",
                            offerLetter.getCandidate().getFirstName(),
                            offerLetter.getCandidate().getLastName(),
                            offerLetter.getJobPosition().getTitle()));
            log.info("Offer {} submitted for approval. First approver: {}",
                    offerId, firstApproval.getApprover().getEmail());
        }

        offerLetter = offerLetterRepository.save(offerLetter);
        return mapToResponse(offerLetter);
    }

    @Transactional
    public OfferLetterResponse processApproval(UUID offerId, String approverEmail, ApprovalDecisionRequest decision) {
        OfferLetter offerLetter = findOfferById(offerId);

        if (offerLetter.getStatus() != OfferStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Offer is not pending approval");
        }

        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", approverEmail));

        List<OfferApproval> approvals = offerApprovalRepository.findByOfferLetterIdOrderByApprovalOrderAsc(offerId);

        // Find the current pending approval for this approver
        OfferApproval currentApproval = approvals.stream()
                .filter(a -> a.getApprover().getId().equals(approver.getId()) && a.getStatus() == ApprovalStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No pending approval found for this approver"));

        // Ensure it's this approver's turn (all previous approvals must be approved)
        boolean allPreviousApproved = approvals.stream()
                .filter(a -> a.getApprovalOrder() < currentApproval.getApprovalOrder())
                .allMatch(a -> a.getStatus() == ApprovalStatus.APPROVED);

        if (!allPreviousApproved) {
            throw new BadRequestException("Previous approvals must be completed before this approver can act");
        }

        currentApproval.setStatus(decision.getApproved() ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        currentApproval.setComments(decision.getComments());
        currentApproval.setRespondedAt(Instant.now());
        offerApprovalRepository.save(currentApproval);

        if (!decision.getApproved()) {
            // Rejected - mark offer as DRAFT again for revision
            offerLetter.setStatus(OfferStatus.DRAFT);
            log.info("Offer {} rejected by {}", offerId, approverEmail);

            emailNotificationService.sendEmail(
                    offerLetter.getCreatedBy().getEmail(),
                    "Offer Letter Rejected",
                    String.format("The offer letter for %s %s was rejected by %s. Comments: %s",
                            offerLetter.getCandidate().getFirstName(),
                            offerLetter.getCandidate().getLastName(),
                            approverEmail,
                            decision.getComments() != null ? decision.getComments() : "None"));
        } else {
            // Check if there are more approvers
            boolean allApproved = approvals.stream()
                    .allMatch(a -> a.getStatus() == ApprovalStatus.APPROVED);

            if (allApproved) {
                offerLetter.setStatus(OfferStatus.APPROVED);
                log.info("Offer {} fully approved", offerId);

                emailNotificationService.sendEmail(
                        offerLetter.getCreatedBy().getEmail(),
                        "Offer Letter Approved",
                        String.format("The offer letter for %s %s has been fully approved and is ready to send.",
                                offerLetter.getCandidate().getFirstName(),
                                offerLetter.getCandidate().getLastName()));
            } else {
                // Notify next approver
                OfferApproval nextApproval = approvals.stream()
                        .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                        .findFirst()
                        .orElse(null);

                if (nextApproval != null) {
                    nextApproval.setRequestedAt(Instant.now());
                    offerApprovalRepository.save(nextApproval);

                    emailNotificationService.sendEmail(
                            nextApproval.getApprover().getEmail(),
                            "Offer Letter Pending Your Approval",
                            String.format("An offer letter for %s %s (position: %s) requires your approval.",
                                    offerLetter.getCandidate().getFirstName(),
                                    offerLetter.getCandidate().getLastName(),
                                    offerLetter.getJobPosition().getTitle()));
                    log.info("Offer {} - next approver notified: {}", offerId, nextApproval.getApprover().getEmail());
                }
            }
        }

        offerLetter = offerLetterRepository.save(offerLetter);
        return mapToResponse(offerLetter);
    }

    @Transactional
    public OfferLetterResponse sendOffer(UUID offerId) {
        OfferLetter offerLetter = findOfferById(offerId);

        if (offerLetter.getStatus() != OfferStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED offers can be sent to candidates");
        }

        offerLetter.setStatus(OfferStatus.SENT);
        offerLetter.setSentAt(Instant.now());

        // Trigger e-signature if configured
        if (offerLetter.getEsignatureProvider() != ESignatureProvider.NONE) {
            ESignatureService esignService = resolveESignatureService(offerLetter.getEsignatureProvider());
            String envelopeId = esignService.sendForSignature(offerLetter);
            offerLetter.setEsignatureEnvelopeId(envelopeId);
            offerLetter.setEsignatureStatus(ESignatureStatus.SENT);
            log.info("Offer {} sent for e-signature via {}. Envelope: {}",
                    offerId, offerLetter.getEsignatureProvider(), envelopeId);
        }

        // Notify candidate
        emailNotificationService.sendEmail(
                offerLetter.getCandidate().getEmail(),
                "You Have a New Offer Letter!",
                String.format("Congratulations %s! You have received an offer for the %s position. " +
                                "Please log in to review and respond to your offer.",
                        offerLetter.getCandidate().getFirstName(),
                        offerLetter.getJobPosition().getTitle()));

        offerLetter = offerLetterRepository.save(offerLetter);
        log.info("Offer {} sent to candidate {}", offerId, offerLetter.getCandidate().getEmail());
        return mapToResponse(offerLetter);
    }

    @Transactional
    public OfferLetterResponse viewOffer(UUID offerId, String candidateEmail) {
        OfferLetter offerLetter = findOfferById(offerId);

        if (!offerLetter.getCandidate().getEmail().equals(candidateEmail)) {
            throw new BadRequestException("This offer does not belong to the specified candidate");
        }

        if (offerLetter.getStatus() == OfferStatus.SENT) {
            offerLetter.setStatus(OfferStatus.VIEWED);
            offerLetter.setViewedAt(Instant.now());
            offerLetter = offerLetterRepository.save(offerLetter);
            log.info("Offer {} viewed by candidate {}", offerId, candidateEmail);
        }

        return mapToResponse(offerLetter);
    }

    @Transactional
    public OfferLetterResponse respondToOffer(UUID offerId, String candidateEmail, boolean accepted, String responseNotes) {
        OfferLetter offerLetter = findOfferById(offerId);

        if (!offerLetter.getCandidate().getEmail().equals(candidateEmail)) {
            throw new BadRequestException("This offer does not belong to the specified candidate");
        }

        if (offerLetter.getStatus() != OfferStatus.SENT && offerLetter.getStatus() != OfferStatus.VIEWED) {
            throw new BadRequestException("Offer must be in SENT or VIEWED status to respond");
        }

        // Check if offer has expired
        if (offerLetter.getExpiresAt() != null && Instant.now().isAfter(offerLetter.getExpiresAt())) {
            offerLetter.setStatus(OfferStatus.EXPIRED);
            offerLetterRepository.save(offerLetter);
            throw new BadRequestException("This offer has expired");
        }

        offerLetter.setStatus(accepted ? OfferStatus.ACCEPTED : OfferStatus.DECLINED);
        offerLetter.setRespondedAt(Instant.now());
        offerLetter.setCandidateResponse(responseNotes);

        // Notify recruiter
        emailNotificationService.sendEmail(
                offerLetter.getCreatedBy().getEmail(),
                accepted ? "Offer Accepted!" : "Offer Declined",
                String.format("Candidate %s %s has %s the offer for %s.%s",
                        offerLetter.getCandidate().getFirstName(),
                        offerLetter.getCandidate().getLastName(),
                        accepted ? "accepted" : "declined",
                        offerLetter.getJobPosition().getTitle(),
                        responseNotes != null ? " Notes: " + responseNotes : ""));

        offerLetter = offerLetterRepository.save(offerLetter);
        log.info("Offer {} {} by candidate {}", offerId, accepted ? "accepted" : "declined", candidateEmail);
        return mapToResponse(offerLetter);
    }

    @Transactional
    public OfferLetterResponse revokeOffer(UUID offerId) {
        OfferLetter offerLetter = findOfferById(offerId);

        if (offerLetter.getStatus() == OfferStatus.ACCEPTED) {
            throw new BadRequestException("Cannot revoke an accepted offer");
        }
        if (offerLetter.getStatus() == OfferStatus.REVOKED) {
            throw new BadRequestException("Offer is already revoked");
        }

        offerLetter.setStatus(OfferStatus.REVOKED);

        // Notify candidate if the offer was already sent
        if (offerLetter.getSentAt() != null) {
            emailNotificationService.sendEmail(
                    offerLetter.getCandidate().getEmail(),
                    "Offer Letter Revoked",
                    String.format("The offer for the %s position has been revoked. " +
                                    "Please contact your recruiter for more information.",
                            offerLetter.getJobPosition().getTitle()));
        }

        offerLetter = offerLetterRepository.save(offerLetter);
        log.info("Offer {} revoked", offerId);
        return mapToResponse(offerLetter);
    }

    @Transactional(readOnly = true)
    public OfferLetterResponse getOffer(UUID offerId) {
        OfferLetter offerLetter = findOfferById(offerId);
        return mapToResponse(offerLetter);
    }

    @Transactional(readOnly = true)
    public List<OfferLetterResponse> getOffersForCandidate(String candidateEmail) {
        List<OfferLetter> offers = offerLetterRepository.findByCandidateEmail(candidateEmail);
        return offers.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfferLetterResponse> getOffersForPosition(UUID positionId) {
        jobPositionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", positionId));

        List<OfferLetter> offers = offerLetterRepository.findByJobPositionIdOrderByCreatedAtDesc(positionId);
        return offers.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public OfferLetterResponse checkESignatureStatus(UUID offerId) {
        OfferLetter offerLetter = findOfferById(offerId);

        if (offerLetter.getEsignatureProvider() == ESignatureProvider.NONE) {
            throw new BadRequestException("No e-signature provider configured for this offer");
        }

        if (offerLetter.getEsignatureEnvelopeId() == null) {
            throw new BadRequestException("E-signature has not been initiated for this offer");
        }

        ESignatureService esignService = resolveESignatureService(offerLetter.getEsignatureProvider());
        ESignatureStatus newStatus = esignService.getSignatureStatus(offerLetter.getEsignatureEnvelopeId());
        offerLetter.setEsignatureStatus(newStatus);

        if (newStatus == ESignatureStatus.SIGNED) {
            offerLetter.setEsignatureSignedAt(Instant.now());
            String documentUrl = esignService.getSignedDocumentUrl(offerLetter.getEsignatureEnvelopeId());
            offerLetter.setEsignatureDocumentUrl(documentUrl);
            log.info("Offer {} e-signature completed", offerId);
        }

        offerLetter = offerLetterRepository.save(offerLetter);
        return mapToResponse(offerLetter);
    }

    // ==================== Private Helpers ====================

    private OfferLetter findOfferById(UUID offerId) {
        return offerLetterRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("OfferLetter", "id", offerId));
    }

    private ESignatureService resolveESignatureService(ESignatureProvider provider) {
        return switch (provider) {
            case DOCUSIGN -> docuSignService;
            case HELLOSIGN -> helloSignService;
            case NONE -> throw new BadRequestException("No e-signature provider configured");
        };
    }

    private OfferLetterResponse mapToResponse(OfferLetter offerLetter) {
        List<OfferApprovalResponse> approvalResponses = offerLetter.getApprovals().stream()
                .map(this::mapApprovalToResponse)
                .collect(Collectors.toList());

        return OfferLetterResponse.builder()
                .id(offerLetter.getId())
                .candidateId(offerLetter.getCandidate().getId())
                .candidateName(offerLetter.getCandidate().getFirstName() + " " + offerLetter.getCandidate().getLastName())
                .candidateEmail(offerLetter.getCandidate().getEmail())
                .jobPositionId(offerLetter.getJobPosition().getId())
                .jobPositionTitle(offerLetter.getJobPosition().getTitle())
                .department(offerLetter.getJobPosition().getDepartment())
                .createdById(offerLetter.getCreatedBy().getId())
                .createdByName(offerLetter.getCreatedBy().getFirstName() + " " + offerLetter.getCreatedBy().getLastName())
                .status(offerLetter.getStatus())
                .offerContent(offerLetter.getOfferContent())
                .salaryOffered(offerLetter.getSalaryOffered())
                .salaryCurrency(offerLetter.getSalaryCurrency())
                .bonusAmount(offerLetter.getBonusAmount())
                .startDate(offerLetter.getStartDate())
                .expiresAt(offerLetter.getExpiresAt())
                .sentAt(offerLetter.getSentAt())
                .viewedAt(offerLetter.getViewedAt())
                .respondedAt(offerLetter.getRespondedAt())
                .candidateResponse(offerLetter.getCandidateResponse())
                .esignatureProvider(offerLetter.getEsignatureProvider())
                .esignatureEnvelopeId(offerLetter.getEsignatureEnvelopeId())
                .esignatureStatus(offerLetter.getEsignatureStatus())
                .esignatureSignedAt(offerLetter.getEsignatureSignedAt())
                .esignatureDocumentUrl(offerLetter.getEsignatureDocumentUrl())
                .createdAt(offerLetter.getCreatedAt())
                .updatedAt(offerLetter.getUpdatedAt())
                .approvals(approvalResponses)
                .build();
    }

    private OfferApprovalResponse mapApprovalToResponse(OfferApproval approval) {
        return OfferApprovalResponse.builder()
                .id(approval.getId())
                .approverId(approval.getApprover().getId())
                .approverName(approval.getApprover().getFirstName() + " " + approval.getApprover().getLastName())
                .approverEmail(approval.getApprover().getEmail())
                .status(approval.getStatus())
                .comments(approval.getComments())
                .approvalOrder(approval.getApprovalOrder())
                .requestedAt(approval.getRequestedAt())
                .respondedAt(approval.getRespondedAt())
                .build();
    }
}

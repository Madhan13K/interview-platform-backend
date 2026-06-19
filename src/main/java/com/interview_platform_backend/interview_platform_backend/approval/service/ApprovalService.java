package com.interview_platform_backend.interview_platform_backend.approval.service;

import com.interview_platform_backend.interview_platform_backend.approval.dto.*;
import com.interview_platform_backend.interview_platform_backend.approval.entity.*;
import com.interview_platform_backend.interview_platform_backend.approval.repository.ApprovalChainRepository;
import com.interview_platform_backend.interview_platform_backend.approval.repository.ApprovalDecisionRepository;
import com.interview_platform_backend.interview_platform_backend.approval.repository.ApprovalRequestRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalChainRepository chainRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public ApprovalService(ApprovalChainRepository chainRepository,
                           ApprovalRequestRepository requestRepository,
                           ApprovalDecisionRepository decisionRepository,
                           UserRepository userRepository,
                           EmailNotificationService emailNotificationService) {
        this.chainRepository = chainRepository;
        this.requestRepository = requestRepository;
        this.decisionRepository = decisionRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public ApprovalChainResponse createChain(CreateApprovalChainRequest request, String creatorEmail) {
        if (chainRepository.existsByNameAndEntityType(request.getName(), request.getEntityType())) {
            throw new DuplicateResourceException("Approval chain with name '" + request.getName() + "' already exists for entity type " + request.getEntityType());
        }

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", creatorEmail));

        ApprovalChain chain = ApprovalChain.builder()
                .name(request.getName())
                .entityType(request.getEntityType())
                .approvalMode(request.getApprovalMode())
                .createdBy(creator.getId())
                .active(true)
                .build();

        List<ApprovalStep> steps = request.getSteps().stream()
                .map(stepReq -> ApprovalStep.builder()
                        .chain(chain)
                        .stepOrder(stepReq.getStepOrder())
                        .approverRole(stepReq.getApproverRole())
                        .approverId(stepReq.getApproverId())
                        .required(stepReq.isRequired())
                        .build())
                .collect(Collectors.toList());

        chain.setSteps(steps);
        ApprovalChain saved = chainRepository.save(chain);

        log.info("Created approval chain '{}' for entity type {} by user {}", saved.getName(), saved.getEntityType(), creatorEmail);
        return mapChainToResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse submitForApproval(ApprovalEntityType entityType, UUID entityId, UUID chainId, String requesterEmail) {
        ApprovalChain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalChain", "id", chainId));

        if (!chain.isActive()) {
            throw new BadRequestException("Approval chain is not active");
        }

        if (!chain.getEntityType().equals(entityType)) {
            throw new BadRequestException("Chain entity type " + chain.getEntityType() + " does not match requested entity type " + entityType);
        }

        boolean alreadyPending = requestRepository.existsByEntityTypeAndEntityIdAndStatusIn(
                entityType, entityId, List.of(ApprovalRequestStatus.PENDING, ApprovalRequestStatus.IN_PROGRESS));
        if (alreadyPending) {
            throw new BadRequestException("An approval request is already pending or in progress for this entity");
        }

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requesterEmail));

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .chain(chain)
                .entityType(entityType)
                .entityId(entityId)
                .status(ApprovalRequestStatus.PENDING)
                .requestedBy(requester)
                .build();

        ApprovalRequest saved = requestRepository.save(approvalRequest);

        // Notify first approver(s) based on mode
        notifyApprovers(saved, chain);

        log.info("Submitted approval request for {} with id {} using chain '{}'", entityType, entityId, chain.getName());
        return mapRequestToResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse processDecision(UUID requestId, SubmitDecisionRequest decisionRequest, String approverEmail) {
        ApprovalRequest approvalRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));

        if (approvalRequest.getStatus() == ApprovalRequestStatus.APPROVED ||
                approvalRequest.getStatus() == ApprovalRequestStatus.REJECTED ||
                approvalRequest.getStatus() == ApprovalRequestStatus.CANCELLED) {
            throw new BadRequestException("Cannot process decision for a request with status: " + approvalRequest.getStatus());
        }

        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", approverEmail));

        ApprovalChain chain = approvalRequest.getChain();
        List<ApprovalStep> steps = chain.getSteps();

        // Find the current step for this approver
        ApprovalStep currentStep = findCurrentStepForApprover(approvalRequest, steps, approver);
        if (currentStep == null) {
            throw new BadRequestException("No pending step found for this approver");
        }

        // Check if already decided this step
        if (decisionRepository.existsByRequestIdAndStepId(requestId, currentStep.getId())) {
            throw new BadRequestException("Decision already submitted for this step");
        }

        ApprovalDecision decision = ApprovalDecision.builder()
                .request(approvalRequest)
                .step(currentStep)
                .approver(approver)
                .decision(decisionRequest.getApproved())
                .comments(decisionRequest.getComments())
                .build();

        decisionRepository.save(decision);

        // Update request status based on mode
        updateRequestStatus(approvalRequest, chain);

        ApprovalRequest updated = requestRepository.save(approvalRequest);
        log.info("Approval decision processed for request {} by approver {}: {}", requestId, approverEmail, decisionRequest.getApproved() ? "APPROVED" : "REJECTED");

        // Notify requester if completed
        if (updated.getStatus() == ApprovalRequestStatus.APPROVED || updated.getStatus() == ApprovalRequestStatus.REJECTED) {
            emailNotificationService.sendEmail(
                    updated.getRequestedBy().getEmail(),
                    "Approval Request " + updated.getStatus(),
                    "Your approval request for " + updated.getEntityType() + " has been " + updated.getStatus().name().toLowerCase() + "."
            );
        }

        return mapRequestToResponse(updated);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getRequestStatus(UUID requestId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));
        return mapRequestToResponse(request);
    }

    @Transactional(readOnly = true)
    public List<ApprovalChainResponse> getChains(ApprovalEntityType entityType) {
        List<ApprovalChain> chains;
        if (entityType != null) {
            chains = chainRepository.findByEntityTypeAndActiveTrue(entityType);
        } else {
            chains = chainRepository.findByActiveTrue();
        }
        return chains.stream().map(this::mapChainToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void cancelRequest(UUID requestId, String userEmail) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));

        if (request.getStatus() == ApprovalRequestStatus.APPROVED || request.getStatus() == ApprovalRequestStatus.REJECTED) {
            throw new BadRequestException("Cannot cancel a request that is already " + request.getStatus());
        }

        if (!request.getRequestedBy().getEmail().equals(userEmail)) {
            throw new BadRequestException("Only the requester can cancel this approval request");
        }

        request.setStatus(ApprovalRequestStatus.CANCELLED);
        request.setCompletedAt(Instant.now());
        requestRepository.save(request);

        log.info("Approval request {} cancelled by {}", requestId, userEmail);
    }

    private ApprovalStep findCurrentStepForApprover(ApprovalRequest request, List<ApprovalStep> steps, User approver) {
        ApprovalChain chain = request.getChain();
        List<ApprovalDecision> existingDecisions = decisionRepository.findByRequestId(request.getId());

        if (chain.getApprovalMode() == ApprovalMode.SEQUENTIAL) {
            return findSequentialStep(steps, existingDecisions, approver);
        }
        return findParallelStep(steps, existingDecisions, approver);
    }

    private ApprovalStep findSequentialStep(List<ApprovalStep> steps, List<ApprovalDecision> decisions, User approver) {
        for (ApprovalStep step : steps) {
            if (isStepDecided(step, decisions)) {
                continue;
            }
            // Sequential: this is the next pending step
            return isApproverForStep(step, approver) ? step : null;
        }
        return null;
    }

    private ApprovalStep findParallelStep(List<ApprovalStep> steps, List<ApprovalDecision> decisions, User approver) {
        for (ApprovalStep step : steps) {
            if (!isStepDecided(step, decisions) && isApproverForStep(step, approver)) {
                return step;
            }
        }
        return null;
    }

    private boolean isStepDecided(ApprovalStep step, List<ApprovalDecision> decisions) {
        return decisions.stream().anyMatch(d -> d.getStep().getId().equals(step.getId()));
    }

    private boolean isApproverForStep(ApprovalStep step, User approver) {
        if (step.getApproverId() != null) {
            return step.getApproverId().equals(approver.getId());
        }
        // Role-based: check if user has the role
        return approver.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName().equalsIgnoreCase(step.getApproverRole()));
    }

    private void updateRequestStatus(ApprovalRequest request, ApprovalChain chain) {
        List<ApprovalDecision> decisions = decisionRepository.findByRequestId(request.getId());
        List<ApprovalStep> requiredSteps = chain.getSteps().stream()
                .filter(ApprovalStep::isRequired)
                .collect(Collectors.toList());

        request.setStatus(ApprovalRequestStatus.IN_PROGRESS);

        switch (chain.getApprovalMode()) {
            case SEQUENTIAL, PARALLEL -> {
                // Check if any required step was rejected
                boolean anyRejected = decisions.stream()
                        .filter(d -> requiredSteps.stream().anyMatch(s -> s.getId().equals(d.getStep().getId())))
                        .anyMatch(d -> !d.isDecision());
                if (anyRejected) {
                    request.setStatus(ApprovalRequestStatus.REJECTED);
                    request.setCompletedAt(Instant.now());
                    return;
                }

                // Check if all required steps are approved
                long approvedRequired = decisions.stream()
                        .filter(d -> d.isDecision())
                        .filter(d -> requiredSteps.stream().anyMatch(s -> s.getId().equals(d.getStep().getId())))
                        .count();
                if (approvedRequired >= requiredSteps.size()) {
                    request.setStatus(ApprovalRequestStatus.APPROVED);
                    request.setCompletedAt(Instant.now());
                }
            }
            case ANY_ONE -> {
                // Any one approval is sufficient
                boolean anyApproved = decisions.stream().anyMatch(ApprovalDecision::isDecision);
                if (anyApproved) {
                    request.setStatus(ApprovalRequestStatus.APPROVED);
                    request.setCompletedAt(Instant.now());
                } else {
                    // All rejected
                    long totalDecisions = decisions.size();
                    long totalSteps = chain.getSteps().size();
                    if (totalDecisions >= totalSteps) {
                        request.setStatus(ApprovalRequestStatus.REJECTED);
                        request.setCompletedAt(Instant.now());
                    }
                }
            }
        }
    }

    private void notifyApprovers(ApprovalRequest request, ApprovalChain chain) {
        List<ApprovalStep> steps = chain.getSteps();
        if (steps.isEmpty()) return;

        if (chain.getApprovalMode() == ApprovalMode.SEQUENTIAL) {
            ApprovalStep firstStep = steps.get(0);
            notifyStepApprover(firstStep, request);
        } else {
            steps.forEach(step -> notifyStepApprover(step, request));
        }
    }

    private void notifyStepApprover(ApprovalStep step, ApprovalRequest request) {
        if (step.getApproverId() != null) {
            userRepository.findById(step.getApproverId()).ifPresent(approver ->
                    emailNotificationService.sendEmail(
                            approver.getEmail(),
                            "Approval Required: " + request.getEntityType(),
                            "You have a pending approval request for " + request.getEntityType() + " (ID: " + request.getEntityId() + "). Please review and provide your decision."
                    )
            );
        }
    }

    private ApprovalChainResponse mapChainToResponse(ApprovalChain chain) {
        List<ApprovalChainResponse.StepResponse> stepResponses = chain.getSteps().stream()
                .map(step -> ApprovalChainResponse.StepResponse.builder()
                        .id(step.getId())
                        .stepOrder(step.getStepOrder())
                        .approverRole(step.getApproverRole())
                        .approverId(step.getApproverId())
                        .required(step.isRequired())
                        .build())
                .collect(Collectors.toList());

        return ApprovalChainResponse.builder()
                .id(chain.getId())
                .name(chain.getName())
                .entityType(chain.getEntityType())
                .approvalMode(chain.getApprovalMode())
                .active(chain.isActive())
                .tenantId(chain.getTenantId())
                .createdBy(chain.getCreatedBy())
                .createdAt(chain.getCreatedAt())
                .steps(stepResponses)
                .build();
    }

    private ApprovalRequestResponse mapRequestToResponse(ApprovalRequest request) {
        List<ApprovalDecision> decisions = decisionRepository.findByRequestId(request.getId());

        List<ApprovalRequestResponse.DecisionResponse> decisionResponses = decisions.stream()
                .map(d -> ApprovalRequestResponse.DecisionResponse.builder()
                        .id(d.getId())
                        .stepId(d.getStep().getId())
                        .stepOrder(d.getStep().getStepOrder())
                        .approverRole(d.getStep().getApproverRole())
                        .approverEmail(d.getApprover().getEmail())
                        .decision(d.isDecision())
                        .comments(d.getComments())
                        .decidedAt(d.getDecidedAt())
                        .build())
                .collect(Collectors.toList());

        return ApprovalRequestResponse.builder()
                .id(request.getId())
                .chainId(request.getChain().getId())
                .chainName(request.getChain().getName())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .status(request.getStatus())
                .requestedByEmail(request.getRequestedBy().getEmail())
                .requestedAt(request.getRequestedAt())
                .completedAt(request.getCompletedAt())
                .decisions(decisionResponses)
                .build();
    }
}

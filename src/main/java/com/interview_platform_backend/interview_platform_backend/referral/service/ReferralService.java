package com.interview_platform_backend.interview_platform_backend.referral.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.jobposition.repository.JobPositionRepository;
import com.interview_platform_backend.interview_platform_backend.notification.EmailNotificationService;
import com.interview_platform_backend.interview_platform_backend.referral.dto.CreateReferralRequest;
import com.interview_platform_backend.interview_platform_backend.referral.dto.ReferralResponse;
import com.interview_platform_backend.interview_platform_backend.referral.dto.ReferralStatsResponse;
import com.interview_platform_backend.interview_platform_backend.referral.entity.Referral;
import com.interview_platform_backend.interview_platform_backend.referral.entity.ReferralStatus;
import com.interview_platform_backend.interview_platform_backend.referral.repository.ReferralRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final JobPositionRepository jobPositionRepository;
    private final EmailNotificationService emailNotificationService;

    public ReferralService(ReferralRepository referralRepository,
                           UserRepository userRepository,
                           JobPositionRepository jobPositionRepository,
                           EmailNotificationService emailNotificationService) {
        this.referralRepository = referralRepository;
        this.userRepository = userRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public ReferralResponse createReferral(CreateReferralRequest request, String referrerEmail) {
        User referrer = userRepository.findByEmail(referrerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", referrerEmail));

        if (referrer.getEmail().equalsIgnoreCase(request.getCandidateEmail())) {
            throw new BadRequestException("You cannot refer yourself");
        }

        if (referralRepository.existsByReferrerIdAndCandidateEmail(referrer.getId(), request.getCandidateEmail())) {
            throw new DuplicateResourceException("You have already referred this candidate");
        }

        JobPosition jobPosition = null;
        if (request.getJobPositionId() != null) {
            jobPosition = jobPositionRepository.findById(request.getJobPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", request.getJobPositionId()));
        }

        String referralCode = generateUniqueReferralCode(referrer);

        Referral referral = Referral.builder()
                .referrer(referrer)
                .candidateEmail(request.getCandidateEmail())
                .candidateName(request.getCandidateName())
                .jobPosition(jobPosition)
                .status(ReferralStatus.SUBMITTED)
                .referralCode(referralCode)
                .notes(request.getNotes())
                .build();

        Referral saved = referralRepository.save(referral);

        // Notify the candidate
        emailNotificationService.sendEmail(
                request.getCandidateEmail(),
                "You've been referred!",
                "Hi " + request.getCandidateName() + ", " + referrer.getFirstName() + " " + referrer.getLastName() +
                        " has referred you for a position. Your referral code is: " + referralCode
        );

        log.info("Referral created by {} for candidate {} with code {}", referrerEmail, request.getCandidateEmail(), referralCode);
        return mapToResponse(saved);
    }

    @Transactional
    public ReferralResponse updateStatus(UUID referralId, ReferralStatus newStatus) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", "id", referralId));

        referral.setStatus(newStatus);
        Referral saved = referralRepository.save(referral);

        // Notify referrer of status change
        emailNotificationService.sendEmail(
                referral.getReferrer().getEmail(),
                "Referral Status Update",
                "Your referral for " + referral.getCandidateName() + " has been updated to: " + newStatus.name()
        );

        log.info("Referral {} status updated to {}", referralId, newStatus);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReferralResponse getReferralByCode(String referralCode) {
        Referral referral = referralRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", "referralCode", referralCode));
        return mapToResponse(referral);
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> getMyReferrals(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return referralRepository.findByReferrerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReferralStatsResponse getReferralStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        UUID userId = user.getId();
        long total = referralRepository.countByReferrerId(userId);
        long hired = referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.HIRED);
        long pending = referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.SUBMITTED) +
                referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.APPLIED) +
                referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.INTERVIEWING);
        BigDecimal totalBonusPaid = referralRepository.sumBonusPaidByReferrerId(userId);
        double conversionRate = total > 0 ? (double) hired / total * 100.0 : 0.0;

        return ReferralStatsResponse.builder()
                .totalReferrals(total)
                .hired(hired)
                .pending(pending)
                .conversionRate(conversionRate)
                .totalBonusPaid(totalBonusPaid != null ? totalBonusPaid : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLeaderboard() {
        List<Object[]> results = referralRepository.findLeaderboard();
        List<Map<String, Object>> leaderboard = new ArrayList<>();

        int rank = 1;
        for (Object[] row : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("userId", row[0]);
            entry.put("firstName", row[1]);
            entry.put("lastName", row[2]);
            entry.put("email", row[3]);
            entry.put("hiredCount", row[4]);
            leaderboard.add(entry);
        }

        return leaderboard;
    }

    @Transactional
    public ReferralResponse markBonusPaid(UUID referralId, BigDecimal amount) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", "id", referralId));

        if (referral.getStatus() != ReferralStatus.HIRED) {
            throw new BadRequestException("Bonus can only be paid for hired referrals");
        }

        referral.setBonusAmount(amount);
        referral.setBonusPaidAt(Instant.now());
        Referral saved = referralRepository.save(referral);

        emailNotificationService.sendEmail(
                referral.getReferrer().getEmail(),
                "Referral Bonus Paid",
                "Congratulations! Your referral bonus of $" + amount + " for " + referral.getCandidateName() + " has been processed."
        );

        log.info("Bonus of {} paid for referral {}", amount, referralId);
        return mapToResponse(saved);
    }

    private String generateUniqueReferralCode(User referrer) {
        String base = "REF-" + referrer.getFirstName().substring(0, Math.min(3, referrer.getFirstName().length())).toUpperCase();
        String code;
        do {
            code = base + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (referralRepository.findByReferralCode(code).isPresent());
        return code;
    }

    private ReferralResponse mapToResponse(Referral referral) {
        return ReferralResponse.builder()
                .id(referral.getId())
                .referrerEmail(referral.getReferrer().getEmail())
                .referrerName(referral.getReferrer().getFirstName() + " " + referral.getReferrer().getLastName())
                .candidateEmail(referral.getCandidateEmail())
                .candidateName(referral.getCandidateName())
                .jobPositionId(referral.getJobPosition() != null ? referral.getJobPosition().getId() : null)
                .jobPositionTitle(referral.getJobPosition() != null ? referral.getJobPosition().getTitle() : null)
                .status(referral.getStatus())
                .referralCode(referral.getReferralCode())
                .bonusAmount(referral.getBonusAmount())
                .bonusPaidAt(referral.getBonusPaidAt())
                .notes(referral.getNotes())
                .createdAt(referral.getCreatedAt())
                .updatedAt(referral.getUpdatedAt())
                .build();
    }
}

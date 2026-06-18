package com.interview_platform_backend.interview_platform_backend.referral.dto;

import com.interview_platform_backend.interview_platform_backend.referral.entity.ReferralStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralResponse {

    private UUID id;
    private String referrerEmail;
    private String referrerName;
    private String candidateEmail;
    private String candidateName;
    private UUID jobPositionId;
    private String jobPositionTitle;
    private ReferralStatus status;
    private String referralCode;
    private BigDecimal bonusAmount;
    private Instant bonusPaidAt;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}

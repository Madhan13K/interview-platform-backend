package com.interview_platform_backend.interview_platform_backend.referral.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralStatsResponse {

    private long totalReferrals;
    private long hired;
    private long pending;
    private double conversionRate;
    private BigDecimal totalBonusPaid;
}

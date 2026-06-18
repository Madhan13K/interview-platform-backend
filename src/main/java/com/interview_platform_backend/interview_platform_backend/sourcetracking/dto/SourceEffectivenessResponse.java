package com.interview_platform_backend.interview_platform_backend.sourcetracking.dto;

import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceEffectivenessResponse {

    private SourceType source;
    private long totalCandidates;
    private long interviewed;
    private long hired;
    private double conversionRate;
    private double avgTimeToHireDays;
    private BigDecimal totalSpend;
    private BigDecimal costPerHire;
    private double roi;
}

package com.interview_platform_backend.interview_platform_backend.sourcetracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceDashboardResponse {

    private List<SourceEffectivenessResponse> sources;
    private long totalCandidatesAllSources;
    private long totalHiredAllSources;
    private BigDecimal totalSpendAllSources;
    private double overallConversionRate;
}

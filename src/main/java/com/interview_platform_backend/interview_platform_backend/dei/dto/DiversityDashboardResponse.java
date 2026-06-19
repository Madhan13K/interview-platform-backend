package com.interview_platform_backend.interview_platform_backend.dei.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiversityDashboardResponse {

    private long totalProfiles;
    private long totalWithConsent;
    private Map<String, Integer> genderDistribution;
    private Map<String, Integer> ethnicityDistribution;
    private Map<String, Integer> ageRangeDistribution;
    private Map<String, Integer> veteranStatusDistribution;
    private Map<String, Integer> disabilityStatusDistribution;
    private List<DiversityFunnelResponse> pipelineBreakdown;
}

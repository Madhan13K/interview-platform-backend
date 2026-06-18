package com.interview_platform_backend.interview_platform_backend.dei.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiversityFunnelResponse {

    private String stage;
    private long totalCandidates;
    private Map<String, Integer> genderDistribution;
    private Map<String, Integer> ethnicityDistribution;
    private Map<String, Integer> ageRangeDistribution;
}

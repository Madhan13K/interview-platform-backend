package com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackSummaryResponse {

    private Long totalResponses;
    private Double averageOverallRating;
    private Double averageCommunicationRating;
    private Double averageProfessionalismRating;
    private Double averageTechnicalClarityRating;
    private Double averageTimelinessRating;
    private Double recommendationRate;
}

package com.interview_platform_backend.interview_platform_backend.candidatefeedback.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateFeedbackResponse {

    private UUID id;
    private UUID interviewId;
    private UUID candidateId;
    private String candidateName;
    private Integer overallRating;
    private Integer communicationRating;
    private Integer professionalismRating;
    private Integer technicalClarityRating;
    private Integer timelinessRating;
    private String comments;
    private Boolean wouldRecommend;
    private Boolean isAnonymous;
    private Instant createdAt;
}

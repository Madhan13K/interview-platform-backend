package com.interview_platform_backend.interview_platform_backend.scorecard.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriteriaResponse {

    private UUID id;
    private String name;
    private String description;
    private InterviewType interviewType;
    private Integer maxScore;
    private Double weight;
    private Integer orderIndex;
    private Boolean isActive;
    private Instant createdAt;
}


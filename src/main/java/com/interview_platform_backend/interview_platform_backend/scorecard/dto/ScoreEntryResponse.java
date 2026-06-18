package com.interview_platform_backend.interview_platform_backend.scorecard.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreEntryResponse {

    private UUID id;
    private UUID criteriaId;
    private String criteriaName;
    private String criteriaDescription;
    private Integer maxScore;
    private Double weight;
    private Integer score;
    private String comments;
}


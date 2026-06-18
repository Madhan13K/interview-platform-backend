package com.interview_platform_backend.interview_platform_backend.scorecard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreEntryRequest {

    @NotNull(message = "Criteria ID is required")
    private UUID criteriaId;

    @NotNull(message = "Score is required")
    @Min(0)
    private Integer score;

    private String comments;
}


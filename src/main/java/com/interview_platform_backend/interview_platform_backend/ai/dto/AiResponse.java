package com.interview_platform_backend.interview_platform_backend.ai.dto;

import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResponse {

    private UUID id;
    private AiSuggestion.AiSuggestionType type;
    private String outputContent;
    private String model;
    private Integer tokensUsed;
    private Double confidenceScore;
    private AiSuggestion.AiSuggestionStatus status;
    private Instant createdAt;
}

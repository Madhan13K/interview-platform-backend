package com.interview_platform_backend.interview_platform_backend.questionbank.dto;

import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionDifficulty;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID id;
    private String title;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private QuestionDifficulty difficulty;
    private QuestionType type;
    private Integer expectedDurationMinutes;
    private String sampleAnswer;
    private String hints;
    private String tags;
    private Boolean isActive;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}


package com.interview_platform_backend.interview_platform_backend.questionbank.dto;

import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionDifficulty;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotNull(message = "Difficulty is required")
    private QuestionDifficulty difficulty;

    @NotNull(message = "Type is required")
    private QuestionType type;

    private Integer expectedDurationMinutes;

    private String sampleAnswer;

    private String hints;

    private String tags;
}


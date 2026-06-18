package com.interview_platform_backend.interview_platform_backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQuestionSuggestionRequest {

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotBlank(message = "Difficulty is required")
    private String difficulty;

    @NotBlank(message = "Category is required")
    private String category;

    private List<String> skills;

    @Builder.Default
    private int count = 5;
}

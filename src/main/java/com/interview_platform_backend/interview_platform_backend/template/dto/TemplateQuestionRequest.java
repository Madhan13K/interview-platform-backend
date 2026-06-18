package com.interview_platform_backend.interview_platform_backend.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateQuestionRequest {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @NotNull(message = "Order index is required")
    private Integer orderIndex;

    private Boolean isMandatory;

    private Integer timeAllocationMinutes;

    private String notes;
}


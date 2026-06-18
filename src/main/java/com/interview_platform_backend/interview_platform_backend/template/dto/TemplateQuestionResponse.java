package com.interview_platform_backend.interview_platform_backend.template.dto;

import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionDifficulty;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateQuestionResponse {

    private UUID id;
    private UUID questionId;
    private String questionTitle;
    private String questionDescription;
    private QuestionDifficulty difficulty;
    private QuestionType questionType;
    private String categoryName;
    private Integer orderIndex;
    private Boolean isMandatory;
    private Integer timeAllocationMinutes;
    private String notes;
}


package com.interview_platform_backend.interview_platform_backend.template.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTemplateRequest {

    private String title;

    private String description;

    private InterviewType type;

    private InterviewMode mode;

    private Integer durationMinutes;

    private String evaluationCriteria;

    private String instructions;

    private String tags;

    private Boolean isActive;

    private List<TemplateQuestionRequest> questions;
}


package com.interview_platform_backend.interview_platform_backend.template.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {

    private UUID id;
    private String title;
    private String description;
    private InterviewType type;
    private InterviewMode mode;
    private Integer durationMinutes;
    private String evaluationCriteria;
    private String instructions;
    private String tags;
    private Boolean isActive;
    private UUID createdById;
    private String createdByName;
    private List<TemplateQuestionResponse> questions;
    private Instant createdAt;
    private Instant updatedAt;
}


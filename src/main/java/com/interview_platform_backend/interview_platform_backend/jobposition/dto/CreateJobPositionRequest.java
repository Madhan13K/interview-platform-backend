package com.interview_platform_backend.interview_platform_backend.jobposition.dto;

import com.interview_platform_backend.interview_platform_backend.jobposition.entity.EmploymentType;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.ExperienceLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobPositionRequest {

    @NotBlank
    private String title;

    private String department;

    private String location;

    @NotNull
    private EmploymentType employmentType;

    @NotNull
    private ExperienceLevel experienceLevel;

    private String description;

    private String requirements;

    private String responsibilities;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String salaryCurrency;

    @Min(1)
    private Integer numberOfOpenings;

    private UUID pipelineId;

    private UUID hiringManagerId;

    private String skills;

    private Instant deadline;
}


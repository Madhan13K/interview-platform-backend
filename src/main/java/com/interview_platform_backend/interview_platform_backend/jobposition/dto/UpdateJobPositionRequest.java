package com.interview_platform_backend.interview_platform_backend.jobposition.dto;

import com.interview_platform_backend.interview_platform_backend.jobposition.entity.EmploymentType;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.ExperienceLevel;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPositionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateJobPositionRequest {

    private String title;
    private String department;
    private String location;
    private EmploymentType employmentType;
    private ExperienceLevel experienceLevel;
    private JobPositionStatus status;
    private String description;
    private String requirements;
    private String responsibilities;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private Integer numberOfOpenings;
    private UUID pipelineId;
    private UUID hiringManagerId;
    private String skills;
    private Instant deadline;
}


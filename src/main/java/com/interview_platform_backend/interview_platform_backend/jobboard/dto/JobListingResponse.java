package com.interview_platform_backend.interview_platform_backend.jobboard.dto;

import com.interview_platform_backend.interview_platform_backend.jobposition.entity.EmploymentType;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.ExperienceLevel;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobListingResponse {

    private UUID id;
    private String title;
    private String department;
    private String location;
    private EmploymentType employmentType;
    private ExperienceLevel experienceLevel;
    private String description;
    private String requirements;
    private String skills;
    private Instant postedAt;
    private Instant deadline;
    private Integer numberOfOpenings;
}

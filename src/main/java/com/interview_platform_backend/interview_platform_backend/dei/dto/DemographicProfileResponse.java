package com.interview_platform_backend.interview_platform_backend.dei.dto;

import com.interview_platform_backend.interview_platform_backend.dei.entity.AgeRange;
import com.interview_platform_backend.interview_platform_backend.dei.entity.Ethnicity;
import com.interview_platform_backend.interview_platform_backend.dei.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemographicProfileResponse {

    private UUID id;
    private UUID userId;
    private Gender gender;
    private Ethnicity ethnicity;
    private Boolean veteranStatus;
    private Boolean disabilityStatus;
    private AgeRange ageRange;
    private boolean consentGiven;
    private Instant consentGivenAt;
    private Instant createdAt;
    private Instant updatedAt;
}

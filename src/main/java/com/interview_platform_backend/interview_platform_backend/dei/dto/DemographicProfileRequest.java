package com.interview_platform_backend.interview_platform_backend.dei.dto;

import com.interview_platform_backend.interview_platform_backend.dei.entity.AgeRange;
import com.interview_platform_backend.interview_platform_backend.dei.entity.Ethnicity;
import com.interview_platform_backend.interview_platform_backend.dei.entity.Gender;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemographicProfileRequest {

    private Gender gender;
    private Ethnicity ethnicity;
    private Boolean veteranStatus;
    private Boolean disabilityStatus;
    private AgeRange ageRange;

    @NotNull(message = "Consent must be explicitly provided")
    private Boolean consentGiven;
}

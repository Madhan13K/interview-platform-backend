package com.interview_platform_backend.interview_platform_backend.jobboard.dto;

import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationSource;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationRequest {

    @NotNull(message = "Job position ID is required")
    private UUID jobPositionId;

    private String coverLetter;

    private String resumeUrl;

    private ApplicationSource source;

    private String referralCode;
}

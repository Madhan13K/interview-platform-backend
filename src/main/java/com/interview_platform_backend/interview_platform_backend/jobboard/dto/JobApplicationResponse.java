package com.interview_platform_backend.interview_platform_backend.jobboard.dto;

import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationSource;
import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationResponse {

    private UUID id;
    private UUID jobPositionId;
    private String jobTitle;
    private String department;
    private String location;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private ApplicationStatus status;
    private String coverLetter;
    private String resumeUrl;
    private ApplicationSource source;
    private String referralCode;
    private String notes;
    private Instant appliedAt;
    private Instant reviewedAt;
    private Instant statusUpdatedAt;
}

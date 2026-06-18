package com.interview_platform_backend.interview_platform_backend.candidate.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponse {
    private UUID id;
    private String title;
    private String description;

    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;

    private UUID scheduledById;
    private String scheduledByName;

    private Instant startTime;
    private Instant endTime;
    private String timeZone;

    private InterviewStatus status;
    private InterviewType type;
    private InterviewMode mode;

    private String meetingLink;
    private String location;
    private String cancelReason;
    private String rescheduleReason;

    private Instant createdAt;
    private Instant updatedAt;

    private List<InterviewInterviewerResponse> interviewers;

}

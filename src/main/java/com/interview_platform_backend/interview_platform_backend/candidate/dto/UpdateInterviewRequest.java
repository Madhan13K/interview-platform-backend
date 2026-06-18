package com.interview_platform_backend.interview_platform_backend.candidate.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInterviewRequest {

    private String title;

    private String description;

    private Instant startTime;

    private Instant endTime;

    private String timeZone;

    private InterviewType type;

    private InterviewMode mode;

    private String meetingLink;

    private String location;

    private List<UUID> interviewerIds;
}


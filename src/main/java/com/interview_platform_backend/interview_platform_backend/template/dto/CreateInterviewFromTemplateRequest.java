package com.interview_platform_backend.interview_platform_backend.template.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInterviewFromTemplateRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private Instant startTime;

    private String timeZone;

    private String meetingLink;

    private String location;

    @NotEmpty(message = "At least one interviewer is required")
    private List<UUID> interviewerIds;
}


package com.interview_platform_backend.interview_platform_backend.candidate.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private UUID candidateId;

    @NotNull
    @Future
    private Instant startTime;

    @NotNull
    @Future
    private Instant endTime;

    private String timeZone;

    @NotNull
    private InterviewType type;

    @NotNull
    private InterviewMode mode;

    private String meetingLink;

    private String location;

    @NotEmpty
    private List<UUID> interviewerIds;
}

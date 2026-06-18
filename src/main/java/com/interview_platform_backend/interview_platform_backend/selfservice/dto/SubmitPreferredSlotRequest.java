package com.interview_platform_backend.interview_platform_backend.selfservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitPreferredSlotRequest {

    @NotNull
    private LocalDate preferredDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private String timeZone;
    private Integer priority;
    private String notes;
    private UUID interviewId;
    private UUID jobPositionId;
}


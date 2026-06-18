package com.interview_platform_backend.interview_platform_backend.calendar.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private UUID id;
    private UUID interviewerId;
    private String interviewerName;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timeZone;
    private Boolean isRecurring;
    private LocalDate specificDate;
    private Instant createdAt;
}


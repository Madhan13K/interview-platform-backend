package com.interview_platform_backend.interview_platform_backend.calendar.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlot {

    private UUID interviewerId;
    private String interviewerName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timeZone;
    private Boolean isAvailable;
}


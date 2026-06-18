package com.interview_platform_backend.interview_platform_backend.scheduling.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilitySlotResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timeZone;
    private Boolean isRecurring;
    private LocalDate specificDate;
    private Boolean isAvailable;
}


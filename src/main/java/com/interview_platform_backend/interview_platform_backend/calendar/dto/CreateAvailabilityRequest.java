package com.interview_platform_backend.interview_platform_backend.calendar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAvailabilityRequest {

    @NotNull
    @Min(0)
    @Max(6)
    private Integer dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private String timeZone;

    private Boolean isRecurring;

    private LocalDate specificDate;
}


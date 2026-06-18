package com.interview_platform_backend.interview_platform_backend.scheduling.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestTimeSlotsRequest {

    @NotEmpty
    private List<UUID> interviewerIds;

    private UUID candidateId;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    @NotNull @Min(15)
    private Integer durationMinutes;

    private String preferredTimeZone;
}


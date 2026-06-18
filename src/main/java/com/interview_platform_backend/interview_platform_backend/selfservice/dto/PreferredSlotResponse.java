package com.interview_platform_backend.interview_platform_backend.selfservice.dto;

import com.interview_platform_backend.interview_platform_backend.selfservice.entity.CandidatePreferredSlot;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferredSlotResponse {
    private UUID id;
    private UUID candidateId;
    private String candidateName;
    private UUID interviewId;
    private UUID jobPositionId;
    private LocalDate preferredDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timeZone;
    private Integer priority;
    private String notes;
    private CandidatePreferredSlot.SlotStatus status;
    private Instant createdAt;
}


package com.interview_platform_backend.interview_platform_backend.scheduling.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestedTimeSlot {
    private Instant startTime;
    private Instant endTime;
    private List<UUID> availableInterviewerIds;
    private List<String> availableInterviewerNames;
    private double score; // higher = better match
}


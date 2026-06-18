package com.interview_platform_backend.interview_platform_backend.calendarsync.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResultResponse {

    private int eventsCreated;
    private int eventsUpdated;
    private int eventsDeleted;
    private List<String> errors;
    private Instant syncedAt;
}

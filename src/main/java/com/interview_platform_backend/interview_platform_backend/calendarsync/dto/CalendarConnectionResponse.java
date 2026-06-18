package com.interview_platform_backend.interview_platform_backend.calendarsync.dto;

import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.CalendarProvider;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarConnectionResponse {

    private UUID id;
    private CalendarProvider provider;
    private String calendarId;
    private boolean syncEnabled;
    private Instant lastSyncAt;
    private Instant connectedAt;
}

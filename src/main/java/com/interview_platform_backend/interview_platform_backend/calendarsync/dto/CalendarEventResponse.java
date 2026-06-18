package com.interview_platform_backend.interview_platform_backend.calendarsync.dto;

import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.CalendarProvider;
import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.SyncDirection;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventResponse {

    private UUID id;
    private UUID interviewId;
    private String interviewTitle;
    private String externalEventId;
    private CalendarProvider provider;
    private Instant lastSyncedAt;
    private SyncDirection syncDirection;
}

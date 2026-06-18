package com.interview_platform_backend.interview_platform_backend.calendarsync.provider;

import java.time.Instant;
import java.util.List;

public record ExternalCalendarEvent(
        String externalEventId,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        String timeZone,
        List<String> attendees,
        String meetingLink
) {
}

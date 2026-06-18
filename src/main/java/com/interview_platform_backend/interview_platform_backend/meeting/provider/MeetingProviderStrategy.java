package com.interview_platform_backend.interview_platform_backend.meeting.provider;

import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingProvider;

import java.time.Instant;

/**
 * Strategy interface for meeting link generation.
 * Implement for each provider (Zoom, Google Meet, Teams, etc.)
 */
public interface MeetingProviderStrategy {

    MeetingProvider getProviderType();

    MeetingDetails generateMeeting(String topic, Instant startTime, Instant endTime, int durationMinutes);

    record MeetingDetails(
            String meetingUrl,
            String hostUrl,
            String meetingId,
            String passcode,
            Instant expiresAt
    ) {}
}


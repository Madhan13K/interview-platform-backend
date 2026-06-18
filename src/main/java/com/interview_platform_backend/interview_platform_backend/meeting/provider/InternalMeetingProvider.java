package com.interview_platform_backend.interview_platform_backend.meeting.provider;

import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Internal meeting link generator — generates platform-hosted meeting URLs.
 * Used as default fallback when external providers are not configured.
 */
@Component
public class InternalMeetingProvider implements MeetingProviderStrategy {

    @Override
    public MeetingProvider getProviderType() {
        return MeetingProvider.INTERNAL;
    }

    @Override
    public MeetingDetails generateMeeting(String topic, Instant startTime, Instant endTime, int durationMinutes) {
        String roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String passcode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String meetingUrl = "https://meet.interview-platform.com/room/" + roomId;
        String hostUrl = meetingUrl + "?role=host&key=" + UUID.randomUUID().toString().substring(0, 8);

        Instant expiresAt = endTime != null
                ? endTime.plus(1, ChronoUnit.HOURS)
                : Instant.now().plus(durationMinutes + 60, ChronoUnit.MINUTES);

        return new MeetingDetails(meetingUrl, hostUrl, roomId, passcode, expiresAt);
    }
}


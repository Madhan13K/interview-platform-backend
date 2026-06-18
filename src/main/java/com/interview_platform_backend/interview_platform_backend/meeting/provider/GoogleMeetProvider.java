package com.interview_platform_backend.interview_platform_backend.meeting.provider;

import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Google Meet link provider.
 * <p>
 * In production, this would use Google Calendar API to create an event with conferencing:
 * POST https://www.googleapis.com/calendar/v3/calendars/primary/events
 * with conferenceData.createRequest
 * <p>
 * For now, generates a simulated Google Meet-style link.
 * Set app.meeting.google.enabled=true and provide OAuth credentials to activate.
 */
@Component
@ConditionalOnProperty(name = "app.meeting.google.enabled", havingValue = "true", matchIfMissing = false)
public class GoogleMeetProvider implements MeetingProviderStrategy {

    @Value("${app.meeting.google.client-id:}")
    private String clientId;

    @Override
    public MeetingProvider getProviderType() {
        return MeetingProvider.GOOGLE_MEET;
    }

    @Override
    public MeetingDetails generateMeeting(String topic, Instant startTime, Instant endTime, int durationMinutes) {
        // TODO: Replace with actual Google Calendar API call
        // Use Google Calendar API with conferenceDataVersion=1

        String meetCode = generateMeetCode();
        String meetingUrl = "https://meet.google.com/" + meetCode;

        Instant expiresAt = endTime != null
                ? endTime.plus(1, ChronoUnit.HOURS)
                : startTime.plus(durationMinutes + 60, ChronoUnit.MINUTES);

        return new MeetingDetails(meetingUrl, meetingUrl, meetCode, null, expiresAt);
    }

    private String generateMeetCode() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i > 0) code.append("-");
            for (int j = 0; j < 4; j++) {
                code.append(chars.charAt((int)(Math.random() * chars.length())));
            }
        }
        return code.toString();
    }
}


package com.interview_platform_backend.interview_platform_backend.meeting.provider;

import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Zoom meeting link provider.
 * <p>
 * In production, this would call the Zoom API:
 * POST https://api.zoom.us/v2/users/me/meetings
 * <p>
 * For now, generates a simulated Zoom-style link.
 * Set app.meeting.zoom.enabled=true and provide API credentials to activate.
 */
@Component
@ConditionalOnProperty(name = "app.meeting.zoom.enabled", havingValue = "true", matchIfMissing = false)
public class ZoomMeetingProvider implements MeetingProviderStrategy {

    @Value("${app.meeting.zoom.api-key:}")
    private String apiKey;

    @Value("${app.meeting.zoom.api-secret:}")
    private String apiSecret;

    @Override
    public MeetingProvider getProviderType() {
        return MeetingProvider.ZOOM;
    }

    @Override
    public MeetingDetails generateMeeting(String topic, Instant startTime, Instant endTime, int durationMinutes) {
        // TODO: Replace with actual Zoom API call
        // POST https://api.zoom.us/v2/users/me/meetings
        // Headers: Authorization: Bearer {jwt_token}
        // Body: { "topic": topic, "type": 2, "start_time": startTime, "duration": durationMinutes }

        String meetingId = String.valueOf(1000000000L + (long)(Math.random() * 9000000000L));
        String passcode = UUID.randomUUID().toString().substring(0, 6);
        String meetingUrl = "https://zoom.us/j/" + meetingId + "?pwd=" + passcode;
        String hostUrl = "https://zoom.us/s/" + meetingId + "?zak=host-token";

        Instant expiresAt = endTime != null
                ? endTime.plus(1, ChronoUnit.HOURS)
                : startTime.plus(durationMinutes + 60, ChronoUnit.MINUTES);

        return new MeetingDetails(meetingUrl, hostUrl, meetingId, passcode, expiresAt);
    }
}


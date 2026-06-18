package com.interview_platform_backend.interview_platform_backend.calendarsync.provider;

import java.time.Instant;
import java.util.List;

public interface CalendarProviderService {

    TokenResponse exchangeCodeForTokens(String authCode, String redirectUri);

    TokenResponse refreshAccessToken(String refreshToken);

    String createEvent(String accessToken, String calendarId, String title, String description,
                       Instant startTime, Instant endTime, String timeZone, List<String> attendees);

    boolean updateEvent(String accessToken, String calendarId, String externalEventId,
                        String title, String description, Instant startTime, Instant endTime, String timeZone);

    boolean deleteEvent(String accessToken, String calendarId, String externalEventId);

    List<ExternalCalendarEvent> getEvents(String accessToken, String calendarId, Instant from, Instant to);
}

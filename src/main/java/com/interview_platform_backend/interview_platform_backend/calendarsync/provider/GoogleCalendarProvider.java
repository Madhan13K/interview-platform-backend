package com.interview_platform_backend.interview_platform_backend.calendarsync.provider;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("googleCalendarProvider")
public class GoogleCalendarProvider implements CalendarProviderService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarProvider.class);

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_API_BASE = "https://www.googleapis.com/calendar/v3";

    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public GoogleCalendarProvider(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public TokenResponse exchangeCodeForTokens(String authCode, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", authCode);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("access_token")) {
            throw new BadRequestException("Failed to exchange authorization code for tokens with Google");
        }

        String accessToken = (String) response.get("access_token");
        String refreshToken = (String) response.get("refresh_token");
        int expiresIn = ((Number) response.get("expires_in")).intValue();
        Instant expiresAt = Instant.now().plusSeconds(expiresIn);

        return new TokenResponse(accessToken, refreshToken, expiresAt);
    }

    @Override
    public TokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", refreshToken);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "refresh_token");

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("access_token")) {
            throw new BadRequestException("Failed to refresh Google access token");
        }

        String accessToken = (String) response.get("access_token");
        int expiresIn = ((Number) response.get("expires_in")).intValue();
        Instant expiresAt = Instant.now().plusSeconds(expiresIn);

        return new TokenResponse(accessToken, refreshToken, expiresAt);
    }

    @Override
    public String createEvent(String accessToken, String calendarId, String title, String description,
                              Instant startTime, Instant endTime, String timeZone, List<String> attendees) {
        String url = CALENDAR_API_BASE + "/calendars/" + calendarId + "/events";

        Map<String, Object> eventBody = buildEventBody(title, description, startTime, endTime, timeZone, attendees);

        Map<String, Object> response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(eventBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("id")) {
            throw new BadRequestException("Failed to create Google Calendar event");
        }

        log.info("Created Google Calendar event with ID: {}", response.get("id"));
        return (String) response.get("id");
    }

    @Override
    public boolean updateEvent(String accessToken, String calendarId, String externalEventId,
                               String title, String description, Instant startTime, Instant endTime, String timeZone) {
        String url = CALENDAR_API_BASE + "/calendars/" + calendarId + "/events/" + externalEventId;

        Map<String, Object> eventBody = buildEventBody(title, description, startTime, endTime, timeZone, null);

        try {
            restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("Updated Google Calendar event: {}", externalEventId);
            return true;
        } catch (Exception e) {
            log.error("Failed to update Google Calendar event: {}", externalEventId, e);
            return false;
        }
    }

    @Override
    public boolean deleteEvent(String accessToken, String calendarId, String externalEventId) {
        String url = CALENDAR_API_BASE + "/calendars/" + calendarId + "/events/" + externalEventId;

        try {
            restClient.delete()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Deleted Google Calendar event: {}", externalEventId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete Google Calendar event: {}", externalEventId, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalCalendarEvent> getEvents(String accessToken, String calendarId, Instant from, Instant to) {
        String url = CALENDAR_API_BASE + "/calendars/" + calendarId + "/events"
                + "?timeMin=" + from.toString()
                + "&timeMax=" + to.toString()
                + "&singleEvents=true"
                + "&orderBy=startTime";

        Map<String, Object> response = restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("items")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        List<ExternalCalendarEvent> events = new ArrayList<>();

        for (Map<String, Object> item : items) {
            events.add(mapToExternalEvent(item));
        }

        return events;
    }

    private Map<String, Object> buildEventBody(String title, String description, Instant startTime,
                                                Instant endTime, String timeZone, List<String> attendees) {
        Map<String, Object> event = new HashMap<>();
        event.put("summary", title);
        if (description != null) {
            event.put("description", description);
        }

        String tz = timeZone != null ? timeZone : "UTC";

        Map<String, String> start = new HashMap<>();
        start.put("dateTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(startTime.atZone(ZoneId.of(tz))));
        start.put("timeZone", tz);
        event.put("start", start);

        Map<String, String> end = new HashMap<>();
        end.put("dateTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(endTime.atZone(ZoneId.of(tz))));
        end.put("timeZone", tz);
        event.put("end", end);

        if (attendees != null && !attendees.isEmpty()) {
            List<Map<String, String>> attendeeList = attendees.stream()
                    .map(email -> {
                        Map<String, String> attendee = new HashMap<>();
                        attendee.put("email", email);
                        return attendee;
                    })
                    .toList();
            event.put("attendees", attendeeList);
        }

        return event;
    }

    @SuppressWarnings("unchecked")
    private ExternalCalendarEvent mapToExternalEvent(Map<String, Object> item) {
        String eventId = (String) item.get("id");
        String summary = (String) item.getOrDefault("summary", "");
        String desc = (String) item.getOrDefault("description", "");

        Map<String, Object> startObj = (Map<String, Object>) item.get("start");
        Map<String, Object> endObj = (Map<String, Object>) item.get("end");

        Instant startTime = parseGoogleDateTime(startObj);
        Instant endTime = parseGoogleDateTime(endObj);
        String tz = startObj != null ? (String) startObj.getOrDefault("timeZone", "UTC") : "UTC";

        List<String> attendeeEmails = new ArrayList<>();
        if (item.containsKey("attendees")) {
            List<Map<String, Object>> attendees = (List<Map<String, Object>>) item.get("attendees");
            for (Map<String, Object> att : attendees) {
                String email = (String) att.get("email");
                if (email != null) {
                    attendeeEmails.add(email);
                }
            }
        }

        String meetingLink = null;
        if (item.containsKey("hangoutLink")) {
            meetingLink = (String) item.get("hangoutLink");
        }

        return new ExternalCalendarEvent(eventId, summary, desc, startTime, endTime, tz, attendeeEmails, meetingLink);
    }

    private Instant parseGoogleDateTime(Map<String, Object> dateTimeObj) {
        if (dateTimeObj == null) {
            return Instant.now();
        }
        String dateTime = (String) dateTimeObj.get("dateTime");
        if (dateTime != null) {
            if (dateTime.contains("Z")) {
                return Instant.parse(dateTime);
            }
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateTime, Instant::from);
        }
        String date = (String) dateTimeObj.get("date");
        if (date != null) {
            return Instant.parse(date + "T00:00:00Z");
        }
        return Instant.now();
    }
}

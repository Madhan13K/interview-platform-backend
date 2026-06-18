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

@Service("outlookCalendarProvider")
public class OutlookCalendarProvider implements CalendarProviderService {

    private static final Logger log = LoggerFactory.getLogger(OutlookCalendarProvider.class);

    private static final String TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String GRAPH_API_BASE = "https://graph.microsoft.com/v1.0";

    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-secret}")
    private String clientSecret;

    public OutlookCalendarProvider(RestClient.Builder restClientBuilder) {
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
        params.add("scope", "Calendars.ReadWrite offline_access");

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("access_token")) {
            throw new BadRequestException("Failed to exchange authorization code for tokens with Microsoft");
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
        params.add("scope", "Calendars.ReadWrite offline_access");

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("access_token")) {
            throw new BadRequestException("Failed to refresh Microsoft access token");
        }

        String accessToken = (String) response.get("access_token");
        String newRefreshToken = (String) response.getOrDefault("refresh_token", refreshToken);
        int expiresIn = ((Number) response.get("expires_in")).intValue();
        Instant expiresAt = Instant.now().plusSeconds(expiresIn);

        return new TokenResponse(accessToken, newRefreshToken, expiresAt);
    }

    @Override
    public String createEvent(String accessToken, String calendarId, String title, String description,
                              Instant startTime, Instant endTime, String timeZone, List<String> attendees) {
        String url = GRAPH_API_BASE + "/me/calendars/" + calendarId + "/events";

        Map<String, Object> eventBody = buildEventBody(title, description, startTime, endTime, timeZone, attendees);

        Map<String, Object> response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(eventBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("id")) {
            throw new BadRequestException("Failed to create Outlook Calendar event");
        }

        log.info("Created Outlook Calendar event with ID: {}", response.get("id"));
        return (String) response.get("id");
    }

    @Override
    public boolean updateEvent(String accessToken, String calendarId, String externalEventId,
                               String title, String description, Instant startTime, Instant endTime, String timeZone) {
        String url = GRAPH_API_BASE + "/me/calendars/" + calendarId + "/events/" + externalEventId;

        Map<String, Object> eventBody = buildEventBody(title, description, startTime, endTime, timeZone, null);

        try {
            restClient.method(HttpMethod.PATCH)
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("Updated Outlook Calendar event: {}", externalEventId);
            return true;
        } catch (Exception e) {
            log.error("Failed to update Outlook Calendar event: {}", externalEventId, e);
            return false;
        }
    }

    @Override
    public boolean deleteEvent(String accessToken, String calendarId, String externalEventId) {
        String url = GRAPH_API_BASE + "/me/calendars/" + calendarId + "/events/" + externalEventId;

        try {
            restClient.delete()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Deleted Outlook Calendar event: {}", externalEventId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete Outlook Calendar event: {}", externalEventId, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalCalendarEvent> getEvents(String accessToken, String calendarId, Instant from, Instant to) {
        String filter = "$filter=start/dateTime ge '" + from.toString() + "' and end/dateTime le '" + to.toString() + "'";
        String url = GRAPH_API_BASE + "/me/calendars/" + calendarId + "/events?" + filter;

        Map<String, Object> response = restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("value")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
        List<ExternalCalendarEvent> events = new ArrayList<>();

        for (Map<String, Object> item : items) {
            events.add(mapToExternalEvent(item));
        }

        return events;
    }

    private Map<String, Object> buildEventBody(String title, String description, Instant startTime,
                                                Instant endTime, String timeZone, List<String> attendees) {
        Map<String, Object> event = new HashMap<>();
        event.put("subject", title);

        if (description != null) {
            Map<String, String> body = new HashMap<>();
            body.put("contentType", "text");
            body.put("content", description);
            event.put("body", body);
        }

        String tz = timeZone != null ? timeZone : "UTC";

        Map<String, String> start = new HashMap<>();
        start.put("dateTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .format(startTime.atZone(ZoneId.of(tz)).toLocalDateTime()));
        start.put("timeZone", tz);
        event.put("start", start);

        Map<String, String> end = new HashMap<>();
        end.put("dateTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .format(endTime.atZone(ZoneId.of(tz)).toLocalDateTime()));
        end.put("timeZone", tz);
        event.put("end", end);

        if (attendees != null && !attendees.isEmpty()) {
            List<Map<String, Object>> attendeeList = attendees.stream()
                    .map(email -> {
                        Map<String, Object> attendee = new HashMap<>();
                        Map<String, String> emailAddress = new HashMap<>();
                        emailAddress.put("address", email);
                        attendee.put("emailAddress", emailAddress);
                        attendee.put("type", "required");
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
        String subject = (String) item.getOrDefault("subject", "");

        String desc = "";
        if (item.containsKey("body")) {
            Map<String, Object> body = (Map<String, Object>) item.get("body");
            desc = (String) body.getOrDefault("content", "");
        }

        Map<String, Object> startObj = (Map<String, Object>) item.get("start");
        Map<String, Object> endObj = (Map<String, Object>) item.get("end");

        Instant startTime = parseOutlookDateTime(startObj);
        Instant endTime = parseOutlookDateTime(endObj);
        String tz = startObj != null ? (String) startObj.getOrDefault("timeZone", "UTC") : "UTC";

        List<String> attendeeEmails = new ArrayList<>();
        if (item.containsKey("attendees")) {
            List<Map<String, Object>> attendees = (List<Map<String, Object>>) item.get("attendees");
            for (Map<String, Object> att : attendees) {
                Map<String, Object> emailAddress = (Map<String, Object>) att.get("emailAddress");
                if (emailAddress != null) {
                    String email = (String) emailAddress.get("address");
                    if (email != null) {
                        attendeeEmails.add(email);
                    }
                }
            }
        }

        String meetingLink = null;
        if (item.containsKey("onlineMeeting") && item.get("onlineMeeting") != null) {
            Map<String, Object> onlineMeeting = (Map<String, Object>) item.get("onlineMeeting");
            meetingLink = (String) onlineMeeting.get("joinUrl");
        }

        return new ExternalCalendarEvent(eventId, subject, desc, startTime, endTime, tz, attendeeEmails, meetingLink);
    }

    private Instant parseOutlookDateTime(Map<String, Object> dateTimeObj) {
        if (dateTimeObj == null) {
            return Instant.now();
        }
        String dateTime = (String) dateTimeObj.get("dateTime");
        String timeZone = (String) dateTimeObj.getOrDefault("timeZone", "UTC");

        if (dateTime != null) {
            try {
                return java.time.LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(ZoneId.of(timeZone))
                        .toInstant();
            } catch (Exception e) {
                return Instant.parse(dateTime);
            }
        }
        return Instant.now();
    }
}

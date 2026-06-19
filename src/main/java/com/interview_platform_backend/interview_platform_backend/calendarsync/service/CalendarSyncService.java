package com.interview_platform_backend.interview_platform_backend.calendarsync.service;

import com.interview_platform_backend.interview_platform_backend.calendarsync.dto.*;
import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.*;
import com.interview_platform_backend.interview_platform_backend.calendarsync.provider.*;
import com.interview_platform_backend.interview_platform_backend.calendarsync.repository.CalendarConnectionRepository;
import com.interview_platform_backend.interview_platform_backend.calendarsync.repository.CalendarEventRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewInterviewer;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarSyncService {

    private static final Logger log = LoggerFactory.getLogger(CalendarSyncService.class);

    private final CalendarConnectionRepository connectionRepository;
    private final CalendarEventRepository eventRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final GoogleCalendarProvider googleCalendarProvider;
    private final OutlookCalendarProvider outlookCalendarProvider;

    public CalendarSyncService(CalendarConnectionRepository connectionRepository,
                               CalendarEventRepository eventRepository,
                               InterviewRepository interviewRepository,
                               UserRepository userRepository,
                               GoogleCalendarProvider googleCalendarProvider,
                               OutlookCalendarProvider outlookCalendarProvider) {
        this.connectionRepository = connectionRepository;
        this.eventRepository = eventRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.googleCalendarProvider = googleCalendarProvider;
        this.outlookCalendarProvider = outlookCalendarProvider;
    }

    @Transactional
    public CalendarConnectionResponse connectCalendar(CalendarConnectionRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);

        if (connectionRepository.existsByUserIdAndProvider(user.getId(), request.getProvider())) {
            throw new BadRequestException("Calendar connection already exists for provider: " + request.getProvider());
        }

        CalendarProviderService provider = getProviderService(request.getProvider());
        TokenResponse tokens = provider.exchangeCodeForTokens(request.getAuthorizationCode(), request.getRedirectUri());

        CalendarConnection connection = CalendarConnection.builder()
                .user(user)
                .provider(request.getProvider())
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenExpiresAt(tokens.expiresAt())
                .calendarId(request.getCalendarId() != null ? request.getCalendarId() : "primary")
                .syncEnabled(true)
                .build();

        connection = connectionRepository.save(connection);
        log.info("Created calendar connection for user {} with provider {}", userEmail, request.getProvider());

        return mapToConnectionResponse(connection);
    }

    @Transactional
    public void disconnectCalendar(UUID connectionId, String userEmail) {
        User user = findUserByEmail(userEmail);
        CalendarConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("CalendarConnection", "id", connectionId));

        if (!connection.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this calendar connection");
        }

        eventRepository.deleteByConnectionId(connectionId);
        connectionRepository.delete(connection);
        log.info("Disconnected calendar connection {} for user {}", connectionId, userEmail);
    }

    @Transactional(readOnly = true)
    public List<CalendarConnectionResponse> getConnections(String userEmail) {
        User user = findUserByEmail(userEmail);
        return connectionRepository.findByUserId(user.getId()).stream()
                .map(this::mapToConnectionResponse)
                .toList();
    }

    @Transactional
    public SyncResultResponse syncInterviewToCalendar(UUID interviewId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        List<CalendarConnection> connections = connectionRepository.findByUserIdAndSyncEnabledTrue(user.getId());
        if (connections.isEmpty()) {
            throw new BadRequestException("No active calendar connections found");
        }

        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (CalendarConnection connection : connections) {
            try {
                String accessToken = ensureValidToken(connection);
                CalendarProviderService provider = getProviderService(connection.getProvider());

                Optional<CalendarEvent> existingEvent = eventRepository
                        .findByConnectionIdAndInterviewId(connection.getId(), interviewId);

                if (existingEvent.isPresent()) {
                    boolean success = provider.updateEvent(
                            accessToken,
                            connection.getCalendarId(),
                            existingEvent.get().getExternalEventId(),
                            interview.getTitle(),
                            interview.getDescription(),
                            interview.getStartTime(),
                            interview.getEndTime(),
                            interview.getTimeZone()
                    );
                    if (success) {
                        existingEvent.get().setLastSyncedAt(Instant.now());
                        eventRepository.save(existingEvent.get());
                        updated++;
                    } else {
                        errors.add("Failed to update event on " + connection.getProvider());
                    }
                } else {
                    List<String> attendees = getInterviewAttendees(interview);
                    String externalEventId = provider.createEvent(
                            accessToken,
                            connection.getCalendarId(),
                            interview.getTitle(),
                            interview.getDescription(),
                            interview.getStartTime(),
                            interview.getEndTime(),
                            interview.getTimeZone(),
                            attendees
                    );

                    CalendarEvent calendarEvent = CalendarEvent.builder()
                            .connection(connection)
                            .interview(interview)
                            .externalEventId(externalEventId)
                            .externalCalendarId(connection.getCalendarId())
                            .syncDirection(SyncDirection.OUTBOUND)
                            .build();
                    eventRepository.save(calendarEvent);
                    created++;
                }

                connection.setLastSyncAt(Instant.now());
                connectionRepository.save(connection);
            } catch (Exception e) {
                log.error("Error syncing interview {} to connection {}", interviewId, connection.getId(), e);
                errors.add("Error syncing to " + connection.getProvider() + ": " + e.getMessage());
            }
        }

        return SyncResultResponse.builder()
                .eventsCreated(created)
                .eventsUpdated(updated)
                .eventsDeleted(0)
                .errors(errors)
                .syncedAt(Instant.now())
                .build();
    }

    @Transactional
    public SyncResultResponse syncAllInterviews(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<CalendarConnection> connections = connectionRepository.findByUserIdAndSyncEnabledTrue(user.getId());

        if (connections.isEmpty()) {
            throw new BadRequestException("No active calendar connections found");
        }

        Instant now = Instant.now();
        Instant futureLimit = now.plus(90, ChronoUnit.DAYS);
        List<Interview> upcomingInterviews = interviewRepository.findByDateRange(now, futureLimit);

        // Filter to interviews where this user is a participant
        List<Interview> userInterviews = upcomingInterviews.stream()
                .filter(i -> isUserParticipant(i, user.getId()))
                .toList();

        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (CalendarConnection connection : connections) {
            try {
                String accessToken = ensureValidToken(connection);
                CalendarProviderService provider = getProviderService(connection.getProvider());

                for (Interview interview : userInterviews) {
                    try {
                        Optional<CalendarEvent> existingEvent = eventRepository
                                .findByConnectionIdAndInterviewId(connection.getId(), interview.getId());

                        if (existingEvent.isPresent()) {
                            boolean success = provider.updateEvent(
                                    accessToken,
                                    connection.getCalendarId(),
                                    existingEvent.get().getExternalEventId(),
                                    interview.getTitle(),
                                    interview.getDescription(),
                                    interview.getStartTime(),
                                    interview.getEndTime(),
                                    interview.getTimeZone()
                            );
                            if (success) {
                                existingEvent.get().setLastSyncedAt(Instant.now());
                                eventRepository.save(existingEvent.get());
                                updated++;
                            }
                        } else {
                            List<String> attendees = getInterviewAttendees(interview);
                            String externalEventId = provider.createEvent(
                                    accessToken,
                                    connection.getCalendarId(),
                                    interview.getTitle(),
                                    interview.getDescription(),
                                    interview.getStartTime(),
                                    interview.getEndTime(),
                                    interview.getTimeZone(),
                                    attendees
                            );

                            CalendarEvent calendarEvent = CalendarEvent.builder()
                                    .connection(connection)
                                    .interview(interview)
                                    .externalEventId(externalEventId)
                                    .externalCalendarId(connection.getCalendarId())
                                    .syncDirection(SyncDirection.OUTBOUND)
                                    .build();
                            eventRepository.save(calendarEvent);
                            created++;
                        }
                    } catch (Exception e) {
                        errors.add("Error syncing interview " + interview.getId() + ": " + e.getMessage());
                    }
                }

                connection.setLastSyncAt(Instant.now());
                connectionRepository.save(connection);
            } catch (Exception e) {
                log.error("Error during bulk sync for connection {}", connection.getId(), e);
                errors.add("Error with connection " + connection.getProvider() + ": " + e.getMessage());
            }
        }

        return SyncResultResponse.builder()
                .eventsCreated(created)
                .eventsUpdated(updated)
                .eventsDeleted(0)
                .errors(errors)
                .syncedAt(Instant.now())
                .build();
    }

    @Transactional
    public SyncResultResponse triggerBidirectionalSync(UUID connectionId, String userEmail) {
        User user = findUserByEmail(userEmail);
        CalendarConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("CalendarConnection", "id", connectionId));

        if (!connection.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this calendar connection");
        }

        String accessToken = ensureValidToken(connection);
        CalendarProviderService provider = getProviderService(connection.getProvider());

        Instant now = Instant.now();
        Instant futureLimit = now.plus(90, ChronoUnit.DAYS);

        int created = 0;
        int updated = 0;
        int deleted = 0;
        List<String> errors = new ArrayList<>();

        // --- OUTBOUND: Push local interviews to external calendar ---
        List<Interview> upcomingInterviews = interviewRepository.findByDateRange(now, futureLimit).stream()
                .filter(i -> isUserParticipant(i, user.getId()))
                .toList();

        for (Interview interview : upcomingInterviews) {
            try {
                Optional<CalendarEvent> existingEvent = eventRepository
                        .findByConnectionIdAndInterviewId(connection.getId(), interview.getId());

                if (existingEvent.isPresent()) {
                    boolean success = provider.updateEvent(
                            accessToken,
                            connection.getCalendarId(),
                            existingEvent.get().getExternalEventId(),
                            interview.getTitle(),
                            interview.getDescription(),
                            interview.getStartTime(),
                            interview.getEndTime(),
                            interview.getTimeZone()
                    );
                    if (success) {
                        existingEvent.get().setLastSyncedAt(Instant.now());
                        eventRepository.save(existingEvent.get());
                        updated++;
                    }
                } else {
                    List<String> attendees = getInterviewAttendees(interview);
                    String externalEventId = provider.createEvent(
                            accessToken,
                            connection.getCalendarId(),
                            interview.getTitle(),
                            interview.getDescription(),
                            interview.getStartTime(),
                            interview.getEndTime(),
                            interview.getTimeZone(),
                            attendees
                    );

                    CalendarEvent calendarEvent = CalendarEvent.builder()
                            .connection(connection)
                            .interview(interview)
                            .externalEventId(externalEventId)
                            .externalCalendarId(connection.getCalendarId())
                            .syncDirection(SyncDirection.OUTBOUND)
                            .build();
                    eventRepository.save(calendarEvent);
                    created++;
                }
            } catch (Exception e) {
                errors.add("Outbound sync error for interview " + interview.getId() + ": " + e.getMessage());
            }
        }

        // --- INBOUND: Pull external events (log for awareness, no auto-create interviews) ---
        try {
            List<ExternalCalendarEvent> externalEvents = provider.getEvents(
                    accessToken, connection.getCalendarId(), now, futureLimit);
            log.info("Pulled {} external events from {} for bidirectional sync",
                    externalEvents.size(), connection.getProvider());
        } catch (Exception e) {
            errors.add("Inbound sync error: " + e.getMessage());
        }

        connection.setLastSyncAt(Instant.now());
        connectionRepository.save(connection);

        return SyncResultResponse.builder()
                .eventsCreated(created)
                .eventsUpdated(updated)
                .eventsDeleted(deleted)
                .errors(errors)
                .syncedAt(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getSyncedEvents(String userEmail) {
        User user = findUserByEmail(userEmail);
        return eventRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToEventResponse)
                .toList();
    }

    @Transactional
    public void handleInterviewUpdate(UUID interviewId) {
        Interview interview = interviewRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        List<CalendarEvent> calendarEvents = eventRepository.findByInterviewId(interviewId);

        for (CalendarEvent calendarEvent : calendarEvents) {
            try {
                CalendarConnection connection = calendarEvent.getConnection();
                String accessToken = ensureValidToken(connection);
                CalendarProviderService provider = getProviderService(connection.getProvider());

                boolean success = provider.updateEvent(
                        accessToken,
                        connection.getCalendarId(),
                        calendarEvent.getExternalEventId(),
                        interview.getTitle(),
                        interview.getDescription(),
                        interview.getStartTime(),
                        interview.getEndTime(),
                        interview.getTimeZone()
                );

                if (success) {
                    calendarEvent.setLastSyncedAt(Instant.now());
                    eventRepository.save(calendarEvent);
                }
            } catch (Exception e) {
                log.error("Failed to propagate interview update to external calendar for event {}",
                        calendarEvent.getId(), e);
            }
        }
    }

    @Transactional
    public void handleInterviewCancellation(UUID interviewId) {
        List<CalendarEvent> calendarEvents = eventRepository.findByInterviewId(interviewId);

        for (CalendarEvent calendarEvent : calendarEvents) {
            try {
                CalendarConnection connection = calendarEvent.getConnection();
                String accessToken = ensureValidToken(connection);
                CalendarProviderService provider = getProviderService(connection.getProvider());

                boolean success = provider.deleteEvent(
                        accessToken,
                        connection.getCalendarId(),
                        calendarEvent.getExternalEventId()
                );

                if (success) {
                    eventRepository.delete(calendarEvent);
                    log.info("Deleted external calendar event {} for cancelled interview {}",
                            calendarEvent.getExternalEventId(), interviewId);
                }
            } catch (Exception e) {
                log.error("Failed to delete external calendar event for cancelled interview {}",
                        interviewId, e);
            }
        }
    }

    // ==================== Helper Methods ====================

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private CalendarProviderService getProviderService(CalendarProvider provider) {
        return switch (provider) {
            case GOOGLE_CALENDAR -> googleCalendarProvider;
            case OUTLOOK -> outlookCalendarProvider;
        };
    }

    private String ensureValidToken(CalendarConnection connection) {
        if (connection.getTokenExpiresAt() != null &&
                connection.getTokenExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return connection.getAccessToken();
        }

        if (connection.getRefreshToken() == null) {
            throw new BadRequestException("Token expired and no refresh token available for connection: " + connection.getId());
        }

        // Synchronized on connection ID to prevent race condition when multiple threads
        // try to refresh the same token simultaneously
        synchronized (("calendar-token-refresh-" + connection.getId()).intern()) {
            // Double-check after acquiring lock (another thread may have already refreshed)
            CalendarConnection freshConnection = connectionRepository.findById(connection.getId()).orElse(connection);
            if (freshConnection.getTokenExpiresAt() != null &&
                    freshConnection.getTokenExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
                connection.setAccessToken(freshConnection.getAccessToken());
                connection.setTokenExpiresAt(freshConnection.getTokenExpiresAt());
                return freshConnection.getAccessToken();
            }

            CalendarProviderService provider = getProviderService(connection.getProvider());
            TokenResponse newTokens = provider.refreshAccessToken(connection.getRefreshToken());

            connection.setAccessToken(newTokens.accessToken());
            if (newTokens.refreshToken() != null) {
                connection.setRefreshToken(newTokens.refreshToken());
            }
            connection.setTokenExpiresAt(newTokens.expiresAt());
            connectionRepository.save(connection);

            return newTokens.accessToken();
        }
    }

    private boolean isUserParticipant(Interview interview, UUID userId) {
        if (interview.getCandidate() != null && interview.getCandidate().getId().equals(userId)) {
            return true;
        }
        if (interview.getScheduledBy() != null && interview.getScheduledBy().getId().equals(userId)) {
            return true;
        }
        if (interview.getInterviewers() != null) {
            return interview.getInterviewers().stream()
                    .anyMatch(ii -> ii.getInterviewer().getId().equals(userId));
        }
        return false;
    }

    private List<String> getInterviewAttendees(Interview interview) {
        List<String> attendees = new ArrayList<>();
        if (interview.getCandidate() != null && interview.getCandidate().getEmail() != null) {
            attendees.add(interview.getCandidate().getEmail());
        }
        if (interview.getInterviewers() != null) {
            for (InterviewInterviewer ii : interview.getInterviewers()) {
                if (ii.getInterviewer() != null && ii.getInterviewer().getEmail() != null) {
                    attendees.add(ii.getInterviewer().getEmail());
                }
            }
        }
        return attendees;
    }

    private CalendarConnectionResponse mapToConnectionResponse(CalendarConnection connection) {
        return CalendarConnectionResponse.builder()
                .id(connection.getId())
                .provider(connection.getProvider())
                .calendarId(connection.getCalendarId())
                .syncEnabled(connection.isSyncEnabled())
                .lastSyncAt(connection.getLastSyncAt())
                .connectedAt(connection.getCreatedAt())
                .build();
    }

    private CalendarEventResponse mapToEventResponse(CalendarEvent event) {
        return CalendarEventResponse.builder()
                .id(event.getId())
                .interviewId(event.getInterview().getId())
                .interviewTitle(event.getInterview().getTitle())
                .externalEventId(event.getExternalEventId())
                .provider(event.getConnection().getProvider())
                .lastSyncedAt(event.getLastSyncedAt())
                .syncDirection(event.getSyncDirection())
                .build();
    }
}

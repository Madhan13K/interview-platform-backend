package com.interview_platform_backend.interview_platform_backend.scheduling.service;

import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.scheduling.dto.*;
import com.interview_platform_backend.interview_platform_backend.scheduling.entity.AvailabilitySlot;
import com.interview_platform_backend.interview_platform_backend.scheduling.repository.AvailabilitySlotRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SchedulingService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public SchedulingService(AvailabilitySlotRepository availabilitySlotRepository,
                              InterviewRepository interviewRepository,
                              UserRepository userRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    public AvailabilitySlotResponse createSlot(CreateAvailabilitySlotRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .user(user)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timeZone(request.getTimeZone() != null ? request.getTimeZone() : "UTC")
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : true)
                .specificDate(request.getSpecificDate())
                .isAvailable(true)
                .build();

        AvailabilitySlot saved = availabilitySlotRepository.save(slot);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getUserAvailability(UUID userId) {
        return availabilitySlotRepository.findByUserIdAndIsAvailableTrue(userId).stream()
                .map(this::mapToResponse).toList();
    }

    public void deleteSlot(UUID slotId, UUID userId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("AvailabilitySlot", "id", slotId));
        if (!slot.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own availability slots");
        }
        availabilitySlotRepository.delete(slot);
    }

    @Transactional(readOnly = true)
    public List<SuggestedTimeSlot> suggestTimeSlots(SuggestTimeSlotsRequest request) {
        List<SuggestedTimeSlot> suggestions = new ArrayList<>();
        int durationMinutes = request.getDurationMinutes();

        for (LocalDate date = request.getFromDate(); !date.isAfter(request.getToDate()); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue() - 1; // 0=Monday

            // Get recurring availability for all interviewers on this day
            List<AvailabilitySlot> slots = availabilitySlotRepository.findRecurringByUsersAndDay(
                    request.getInterviewerIds(), dayOfWeek);

            // Also check specific date availability
            for (UUID interviewerId : request.getInterviewerIds()) {
                List<AvailabilitySlot> specificSlots = availabilitySlotRepository.findByUserIdAndSpecificDate(interviewerId, date);
                slots.addAll(specificSlots);
            }

            // Find overlapping time windows across interviewers
            List<SuggestedTimeSlot> daySuggestions = findOverlappingSlots(slots, date, durationMinutes, request);

            // Check against existing interview conflicts
            for (SuggestedTimeSlot suggestion : daySuggestions) {
                List<UUID> available = new ArrayList<>();
                for (UUID interviewerId : suggestion.getAvailableInterviewerIds()) {
                    boolean hasConflict = interviewRepository.existsByInterviewerAndTimeRange(
                            interviewerId, suggestion.getStartTime(), suggestion.getEndTime());
                    if (!hasConflict) available.add(interviewerId);
                }
                if (!available.isEmpty()) {
                    suggestion.setAvailableInterviewerIds(available);
                    suggestion.setAvailableInterviewerNames(
                            available.stream().map(id -> userRepository.findById(id)
                                    .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown")).toList());
                    suggestion.setScore((double) available.size() / request.getInterviewerIds().size() * 100);
                    suggestions.add(suggestion);
                }
            }
        }

        // Sort by score descending
        suggestions.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return suggestions.stream().limit(20).toList();
    }

    private List<SuggestedTimeSlot> findOverlappingSlots(List<AvailabilitySlot> slots, LocalDate date,
                                                          int durationMinutes, SuggestTimeSlotsRequest request) {
        List<SuggestedTimeSlot> results = new ArrayList<>();
        String tz = request.getPreferredTimeZone() != null ? request.getPreferredTimeZone() : "UTC";
        ZoneId zoneId = ZoneId.of(tz);

        // Group by time window and find intersections
        Map<UUID, List<AvailabilitySlot>> byUser = slots.stream()
                .collect(Collectors.groupingBy(s -> s.getUser().getId()));

        // Generate candidate time slots in 30-min increments from 8:00 to 18:00
        for (int hour = 8; hour < 18; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                LocalTime slotStart = LocalTime.of(hour, minute);
                LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);
                if (slotEnd.isAfter(LocalTime.of(18, 0))) continue;

                List<UUID> availableForSlot = new ArrayList<>();
                for (Map.Entry<UUID, List<AvailabilitySlot>> entry : byUser.entrySet()) {
                    boolean isAvailable = entry.getValue().stream().anyMatch(s ->
                            !s.getStartTime().isAfter(slotStart) && !s.getEndTime().isBefore(slotEnd));
                    if (isAvailable) availableForSlot.add(entry.getKey());
                }

                if (!availableForSlot.isEmpty()) {
                    Instant start = date.atTime(slotStart).atZone(zoneId).toInstant();
                    Instant end = date.atTime(slotEnd).atZone(zoneId).toInstant();
                    results.add(SuggestedTimeSlot.builder()
                            .startTime(start).endTime(end)
                            .availableInterviewerIds(availableForSlot)
                            .build());
                }
            }
        }
        return results;
    }

    private AvailabilitySlotResponse mapToResponse(AvailabilitySlot slot) {
        return AvailabilitySlotResponse.builder()
                .id(slot.getId())
                .userId(slot.getUser().getId())
                .userName(slot.getUser().getFirstName() + " " + slot.getUser().getLastName())
                .dayOfWeek(slot.getDayOfWeek())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .timeZone(slot.getTimeZone())
                .isRecurring(slot.getIsRecurring())
                .specificDate(slot.getSpecificDate())
                .isAvailable(slot.getIsAvailable())
                .build();
    }
}


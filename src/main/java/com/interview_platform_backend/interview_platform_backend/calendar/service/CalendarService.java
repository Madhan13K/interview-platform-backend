package com.interview_platform_backend.interview_platform_backend.calendar.service;

import com.interview_platform_backend.interview_platform_backend.calendar.dto.AvailabilityResponse;
import com.interview_platform_backend.interview_platform_backend.calendar.dto.AvailabilitySlot;
import com.interview_platform_backend.interview_platform_backend.calendar.dto.CreateAvailabilityRequest;
import com.interview_platform_backend.interview_platform_backend.calendar.entity.InterviewerAvailability;
import com.interview_platform_backend.interview_platform_backend.calendar.repository.InterviewerAvailabilityRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CalendarService {

    private final InterviewerAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;

    public CalendarService(InterviewerAvailabilityRepository availabilityRepository,
                           UserRepository userRepository,
                           InterviewRepository interviewRepository) {
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.interviewRepository = interviewRepository;
    }

    public AvailabilityResponse addAvailability(UUID interviewerId, CreateAvailabilityRequest request) {
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().equals(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        InterviewerAvailability availability = InterviewerAvailability.builder()
                .interviewer(interviewer)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timeZone(request.getTimeZone() != null ? request.getTimeZone() : "UTC")
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : true)
                .specificDate(request.getSpecificDate())
                .build();

        InterviewerAvailability saved = availabilityRepository.save(availability);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(UUID interviewerId) {
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        return availabilityRepository.findByInterviewer(interviewer)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getAvailabilityForDate(UUID interviewerId, LocalDate date) {
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", interviewerId));

        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Convert to 0=Sunday

        List<InterviewerAvailability> slots = availabilityRepository
                .findAvailabilityForDate(interviewerId, dayOfWeek, date);

        // Check existing interviews on that date to mark conflicts
        return slots.stream()
                .map(slot -> {
                    ZoneId zone = ZoneId.of(slot.getTimeZone());
                    Instant slotStart = ZonedDateTime.of(date, slot.getStartTime(), zone).toInstant();
                    Instant slotEnd = ZonedDateTime.of(date, slot.getEndTime(), zone).toInstant();

                    boolean hasConflict = interviewRepository
                            .existsByInterviewerAndTimeRange(interviewerId, slotStart, slotEnd);

                    return AvailabilitySlot.builder()
                            .interviewerId(interviewerId)
                            .interviewerName(interviewer.getFirstName() + " " + interviewer.getLastName())
                            .date(date)
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .timeZone(slot.getTimeZone())
                            .isAvailable(!hasConflict)
                            .build();
                })
                .toList();
    }

    public void deleteAvailability(UUID interviewerId, UUID availabilityId) {
        InterviewerAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability", "id", availabilityId));

        if (!availability.getInterviewer().getId().equals(interviewerId)) {
            throw new BadRequestException("Availability does not belong to this interviewer");
        }

        availabilityRepository.delete(availability);
    }

    private AvailabilityResponse toResponse(InterviewerAvailability entity) {
        return AvailabilityResponse.builder()
                .id(entity.getId())
                .interviewerId(entity.getInterviewer().getId())
                .interviewerName(entity.getInterviewer().getFirstName() + " " + entity.getInterviewer().getLastName())
                .dayOfWeek(entity.getDayOfWeek())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .timeZone(entity.getTimeZone())
                .isRecurring(entity.getIsRecurring())
                .specificDate(entity.getSpecificDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}


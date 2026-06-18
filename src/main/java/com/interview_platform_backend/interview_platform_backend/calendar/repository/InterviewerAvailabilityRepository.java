package com.interview_platform_backend.interview_platform_backend.calendar.repository;

import com.interview_platform_backend.interview_platform_backend.calendar.entity.InterviewerAvailability;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InterviewerAvailabilityRepository extends JpaRepository<InterviewerAvailability, UUID> {

    List<InterviewerAvailability> findByInterviewer(User interviewer);

    List<InterviewerAvailability> findByInterviewerIdAndIsRecurringTrue(UUID interviewerId);

    List<InterviewerAvailability> findByInterviewerIdAndSpecificDate(UUID interviewerId, LocalDate date);

    @Query("SELECT a FROM InterviewerAvailability a WHERE a.interviewer.id = :interviewerId " +
            "AND (a.isRecurring = true AND a.dayOfWeek = :dayOfWeek) " +
            "OR (a.specificDate = :date AND a.interviewer.id = :interviewerId)")
    List<InterviewerAvailability> findAvailabilityForDate(
            @Param("interviewerId") UUID interviewerId,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("date") LocalDate date);

    void deleteByInterviewerAndId(User interviewer, UUID id);
}


package com.interview_platform_backend.interview_platform_backend.reminder.repository;

import com.interview_platform_backend.interview_platform_backend.reminder.entity.InterviewReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewReminderRepository extends JpaRepository<InterviewReminder, UUID> {

    List<InterviewReminder> findByInterviewId(UUID interviewId);

    List<InterviewReminder> findByUserId(UUID userId);

    @Query("SELECT r FROM InterviewReminder r WHERE r.status = 'PENDING' AND r.scheduledAt <= :now")
    List<InterviewReminder> findPendingDueReminders(@Param("now") Instant now);

    @Query("SELECT r FROM InterviewReminder r WHERE r.interview.id = :interviewId AND r.status = 'PENDING'")
    List<InterviewReminder> findPendingByInterview(@Param("interviewId") UUID interviewId);

    void deleteByInterviewId(UUID interviewId);
}


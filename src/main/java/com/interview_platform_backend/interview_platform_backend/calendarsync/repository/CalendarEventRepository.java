package com.interview_platform_backend.interview_platform_backend.calendarsync.repository;

import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.CalendarConnection;
import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    List<CalendarEvent> findByConnectionId(UUID connectionId);

    List<CalendarEvent> findByInterviewId(UUID interviewId);

    Optional<CalendarEvent> findByConnectionIdAndInterviewId(UUID connectionId, UUID interviewId);

    Optional<CalendarEvent> findByConnectionAndExternalEventId(CalendarConnection connection, String externalEventId);

    @Query("""
        SELECT ce FROM CalendarEvent ce
        JOIN FETCH ce.connection c
        JOIN FETCH ce.interview i
        WHERE c.user.id = :userId
    """)
    List<CalendarEvent> findAllByUserId(@Param("userId") UUID userId);

    void deleteByInterviewId(UUID interviewId);

    void deleteByConnectionId(UUID connectionId);
}

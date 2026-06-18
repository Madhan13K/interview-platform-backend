package com.interview_platform_backend.interview_platform_backend.calendarsync.repository;

import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.CalendarConnection;
import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.CalendarProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarConnectionRepository extends JpaRepository<CalendarConnection, UUID> {

    List<CalendarConnection> findByUserId(UUID userId);

    Optional<CalendarConnection> findByUserIdAndProvider(UUID userId, CalendarProvider provider);

    List<CalendarConnection> findByUserIdAndSyncEnabledTrue(UUID userId);

    boolean existsByUserIdAndProvider(UUID userId, CalendarProvider provider);
}

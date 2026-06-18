package com.interview_platform_backend.interview_platform_backend.scheduling.repository;

import com.interview_platform_backend.interview_platform_backend.scheduling.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    List<AvailabilitySlot> findByUserId(UUID userId);

    List<AvailabilitySlot> findByUserIdAndIsAvailableTrue(UUID userId);

    List<AvailabilitySlot> findByUserIdAndDayOfWeek(UUID userId, Integer dayOfWeek);

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.user.id = :userId AND a.specificDate = :date AND a.isAvailable = true")
    List<AvailabilitySlot> findByUserIdAndSpecificDate(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.user.id IN :userIds AND a.dayOfWeek = :dayOfWeek AND a.isAvailable = true AND a.isRecurring = true")
    List<AvailabilitySlot> findRecurringByUsersAndDay(@Param("userIds") List<UUID> userIds, @Param("dayOfWeek") Integer dayOfWeek);

    void deleteByUserIdAndId(UUID userId, UUID id);
}


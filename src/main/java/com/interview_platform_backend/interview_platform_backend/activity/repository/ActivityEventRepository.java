package com.interview_platform_backend.interview_platform_backend.activity.repository;

import com.interview_platform_backend.interview_platform_backend.activity.entity.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    List<ActivityEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    Page<ActivityEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<ActivityEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Query(value = """
        SELECT * FROM activity_events ae
        WHERE (CAST(:entityType AS VARCHAR) IS NULL OR ae.entity_type = :entityType)
          AND (CAST(:entityId AS UUID) IS NULL OR ae.entity_id = CAST(:entityId AS UUID))
          AND (CAST(:actorId AS UUID) IS NULL OR ae.actor_id = CAST(:actorId AS UUID))
          AND (CAST(:action AS VARCHAR) IS NULL OR ae.action = :action)
          AND (CAST(:startDate AS TIMESTAMPTZ) IS NULL OR ae.created_at >= CAST(:startDate AS TIMESTAMPTZ))
          AND (CAST(:endDate AS TIMESTAMPTZ) IS NULL OR ae.created_at <= CAST(:endDate AS TIMESTAMPTZ))
        ORDER BY ae.created_at DESC
    """, nativeQuery = true)
    Page<ActivityEvent> findWithFilters(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    Page<ActivityEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

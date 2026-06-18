package com.interview_platform_backend.interview_platform_backend.tag.repository;

import com.interview_platform_backend.interview_platform_backend.tag.entity.EntityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntityTagRepository extends JpaRepository<EntityTag, UUID> {

    List<EntityTag> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<EntityTag> findByTagId(UUID tagId);

    @Query("SELECT et FROM EntityTag et JOIN FETCH et.tag WHERE et.entityType = :entityType AND et.entityId = :entityId")
    List<EntityTag> findWithTagByEntity(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Query("SELECT et.entityId FROM EntityTag et WHERE et.tag.id = :tagId AND et.entityType = :entityType")
    List<UUID> findEntityIdsByTagAndType(@Param("tagId") UUID tagId, @Param("entityType") String entityType);

    boolean existsByTagIdAndEntityTypeAndEntityId(UUID tagId, String entityType, UUID entityId);

    void deleteByTagIdAndEntityTypeAndEntityId(UUID tagId, String entityType, UUID entityId);
}


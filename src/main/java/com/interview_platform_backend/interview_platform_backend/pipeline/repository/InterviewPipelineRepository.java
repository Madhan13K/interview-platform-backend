package com.interview_platform_backend.interview_platform_backend.pipeline.repository;

import com.interview_platform_backend.interview_platform_backend.pipeline.entity.InterviewPipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewPipelineRepository extends JpaRepository<InterviewPipeline, UUID> {

    boolean existsByName(String name);

    List<InterviewPipeline> findByIsActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT p FROM InterviewPipeline p LEFT JOIN FETCH p.stages WHERE p.id = :id")
    Optional<InterviewPipeline> findByIdWithStages(@Param("id") UUID id);

    List<InterviewPipeline> findByDepartmentAndIsActiveTrue(String department);
}


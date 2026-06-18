package com.interview_platform_backend.interview_platform_backend.pipeline.repository;

import com.interview_platform_backend.interview_platform_backend.pipeline.entity.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {

    List<PipelineStage> findByPipelineIdOrderByOrderIndex(UUID pipelineId);
}


package com.interview_platform_backend.interview_platform_backend.pipeline.repository;

import com.interview_platform_backend.interview_platform_backend.pipeline.entity.CandidateStageProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateStageProgressRepository extends JpaRepository<CandidateStageProgress, UUID> {

    List<CandidateStageProgress> findByCandidatePipelineIdOrderByStageOrderIndex(UUID candidatePipelineId);
}


package com.interview_platform_backend.interview_platform_backend.pipeline.repository;

import com.interview_platform_backend.interview_platform_backend.pipeline.entity.CandidatePipeline;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.CandidatePipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidatePipelineRepository extends JpaRepository<CandidatePipeline, UUID> {

    boolean existsByPipelineIdAndCandidateId(UUID pipelineId, UUID candidateId);

    @Query("SELECT cp FROM CandidatePipeline cp " +
            "LEFT JOIN FETCH cp.stageProgress sp " +
            "LEFT JOIN FETCH sp.stage " +
            "LEFT JOIN FETCH cp.currentStage " +
            "LEFT JOIN FETCH cp.candidate " +
            "LEFT JOIN FETCH cp.pipeline " +
            "WHERE cp.id = :id")
    Optional<CandidatePipeline> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT cp FROM CandidatePipeline cp " +
            "LEFT JOIN FETCH cp.currentStage " +
            "LEFT JOIN FETCH cp.candidate " +
            "LEFT JOIN FETCH cp.stageProgress sp " +
            "LEFT JOIN FETCH sp.stage " +
            "WHERE cp.pipeline.id = :pipelineId")
    List<CandidatePipeline> findByPipelineId(@Param("pipelineId") UUID pipelineId);

    @Query("SELECT cp FROM CandidatePipeline cp " +
            "LEFT JOIN FETCH cp.stageProgress sp " +
            "LEFT JOIN FETCH sp.stage " +
            "LEFT JOIN FETCH cp.currentStage " +
            "LEFT JOIN FETCH cp.pipeline p " +
            "LEFT JOIN FETCH p.stages " +
            "WHERE cp.candidate.id = :candidateId")
    List<CandidatePipeline> findByCandidateId(@Param("candidateId") UUID candidateId);

    List<CandidatePipeline> findByPipelineIdAndStatus(UUID pipelineId, CandidatePipelineStatus status);
}


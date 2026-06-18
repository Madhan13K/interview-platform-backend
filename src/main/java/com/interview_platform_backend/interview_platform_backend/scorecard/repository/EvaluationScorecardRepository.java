package com.interview_platform_backend.interview_platform_backend.scorecard.repository;

import com.interview_platform_backend.interview_platform_backend.scorecard.entity.EvaluationScorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationScorecardRepository extends JpaRepository<EvaluationScorecard, UUID> {

    boolean existsByInterviewIdAndInterviewerId(UUID interviewId, UUID interviewerId);

    @Query("SELECT s FROM EvaluationScorecard s LEFT JOIN FETCH s.entries e LEFT JOIN FETCH e.criteria " +
            "WHERE s.interview.id = :interviewId AND s.interviewer.id = :interviewerId")
    Optional<EvaluationScorecard> findByInterviewIdAndInterviewerIdWithEntries(
            @Param("interviewId") UUID interviewId, @Param("interviewerId") UUID interviewerId);

    @Query("SELECT s FROM EvaluationScorecard s LEFT JOIN FETCH s.entries e LEFT JOIN FETCH e.criteria " +
            "WHERE s.interview.id = :interviewId")
    List<EvaluationScorecard> findByInterviewIdWithEntries(@Param("interviewId") UUID interviewId);

    @Query("SELECT s FROM EvaluationScorecard s LEFT JOIN FETCH s.entries e LEFT JOIN FETCH e.criteria " +
            "WHERE s.interviewer.id = :interviewerId")
    List<EvaluationScorecard> findByInterviewerIdWithEntries(@Param("interviewerId") UUID interviewerId);

    @Query("SELECT s FROM EvaluationScorecard s LEFT JOIN FETCH s.entries e LEFT JOIN FETCH e.criteria " +
            "WHERE s.interview.candidate.id = :candidateId")
    List<EvaluationScorecard> findByCandidateIdWithEntries(@Param("candidateId") UUID candidateId);

    @Query("SELECT s FROM EvaluationScorecard s LEFT JOIN FETCH s.entries e LEFT JOIN FETCH e.criteria " +
            "WHERE s.id = :id")
    Optional<EvaluationScorecard> findByIdWithEntries(@Param("id") UUID id);
}


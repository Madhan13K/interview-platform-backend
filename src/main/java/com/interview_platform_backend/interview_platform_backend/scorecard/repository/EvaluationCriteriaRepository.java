package com.interview_platform_backend.interview_platform_backend.scorecard.repository;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.scorecard.entity.EvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, UUID> {

    boolean existsByName(String name);

    List<EvaluationCriteria> findByIsActiveTrueOrderByOrderIndex();

    List<EvaluationCriteria> findByInterviewTypeAndIsActiveTrueOrderByOrderIndex(InterviewType interviewType);

    List<EvaluationCriteria> findByInterviewTypeIsNullAndIsActiveTrueOrderByOrderIndex();
}


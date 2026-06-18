package com.interview_platform_backend.interview_platform_backend.candidatefeedback.repository;

import com.interview_platform_backend.interview_platform_backend.candidatefeedback.entity.CandidateFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateFeedbackRepository extends JpaRepository<CandidateFeedback, UUID> {

    List<CandidateFeedback> findByInterviewId(UUID interviewId);

    List<CandidateFeedback> findByCandidateId(UUID candidateId);

    Page<CandidateFeedback> findByCandidateId(UUID candidateId, Pageable pageable);

    boolean existsByInterviewIdAndCandidateId(UUID interviewId, UUID candidateId);

    @Query("SELECT AVG(cf.overallRating) FROM CandidateFeedback cf")
    Double findAverageOverallRating();

    @Query("SELECT AVG(cf.communicationRating) FROM CandidateFeedback cf WHERE cf.communicationRating IS NOT NULL")
    Double findAverageCommunicationRating();

    @Query("SELECT AVG(cf.professionalismRating) FROM CandidateFeedback cf WHERE cf.professionalismRating IS NOT NULL")
    Double findAverageProfessionalismRating();

    @Query("SELECT AVG(cf.technicalClarityRating) FROM CandidateFeedback cf WHERE cf.technicalClarityRating IS NOT NULL")
    Double findAverageTechnicalClarityRating();

    @Query("SELECT AVG(cf.timelinessRating) FROM CandidateFeedback cf WHERE cf.timelinessRating IS NOT NULL")
    Double findAverageTimelinessRating();

    @Query("SELECT COUNT(cf) FROM CandidateFeedback cf WHERE cf.wouldRecommend = true")
    Long countByWouldRecommendTrue();
}

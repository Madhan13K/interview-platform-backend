package com.interview_platform_backend.interview_platform_backend.sourcetracking.repository;

import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.CandidateSource;
import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateSourceRepository extends JpaRepository<CandidateSource, UUID> {

    List<CandidateSource> findBySource(SourceType source);

    Optional<CandidateSource> findByApplicationId(UUID applicationId);

    boolean existsByApplicationId(UUID applicationId);

    long countBySource(SourceType source);

    @Query("SELECT cs.source, COUNT(cs) FROM CandidateSource cs GROUP BY cs.source")
    List<Object[]> countGroupedBySource();

    @Query("SELECT cs.source, COUNT(cs) FROM CandidateSource cs " +
            "JOIN cs.application app WHERE app.status = 'HIRED' GROUP BY cs.source")
    List<Object[]> countHiredGroupedBySource();

    @Query("SELECT cs.source, COUNT(cs) FROM CandidateSource cs " +
            "JOIN cs.application app WHERE app.status = 'INTERVIEW_SCHEDULED' GROUP BY cs.source")
    List<Object[]> countInterviewedGroupedBySource();

    @Query("SELECT cs.source, COALESCE(SUM(cs.totalSpend), 0) FROM CandidateSource cs GROUP BY cs.source")
    List<Object[]> sumSpendGroupedBySource();

    @Query("SELECT cs FROM CandidateSource cs JOIN cs.application app WHERE app.status = 'HIRED' AND cs.source = :source")
    List<CandidateSource> findHiredBySource(@Param("source") SourceType source);
}

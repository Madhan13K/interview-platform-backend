package com.interview_platform_backend.interview_platform_backend.candidate.repository;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    @Query("""
        SELECT DISTINCT i
        FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.scheduledBy
        LEFT JOIN FETCH i.interviewers ii
        LEFT JOIN FETCH ii.interviewer
        WHERE i.id = :id
    """)
    Optional<Interview> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT DISTINCT i
        FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.scheduledBy
        LEFT JOIN FETCH i.interviewers ii
        LEFT JOIN FETCH ii.interviewer
    """)
    List<Interview> findAllWithDetails();

    @Query(value = """
        SELECT DISTINCT i
        FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.scheduledBy
        LEFT JOIN FETCH i.interviewers ii
        LEFT JOIN FETCH ii.interviewer
        WHERE i.id IN :ids
    """)
    List<Interview> findAllWithDetailsByIds(@Param("ids") List<UUID> ids);

    @Query(value = "SELECT i.id FROM Interview i ORDER BY i.startTime DESC",
           countQuery = "SELECT COUNT(i) FROM Interview i")
    Page<UUID> findAllIds(Pageable pageable);

    @Query("""
        SELECT DISTINCT i
        FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.scheduledBy
        LEFT JOIN FETCH i.interviewers ii
        LEFT JOIN FETCH ii.interviewer
        WHERE i.candidate.id = :candidateId
    """)
    List<Interview> findByCandidateId(@Param("candidateId") UUID candidateId);

    @Query(value = "SELECT i.id FROM Interview i WHERE i.candidate.id = :candidateId ORDER BY i.startTime DESC",
           countQuery = "SELECT COUNT(i) FROM Interview i WHERE i.candidate.id = :candidateId")
    Page<UUID> findIdsByCandidateId(@Param("candidateId") UUID candidateId, Pageable pageable);

    @Query("""
        SELECT DISTINCT i
        FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.scheduledBy
        LEFT JOIN FETCH i.interviewers ii
        LEFT JOIN FETCH ii.interviewer
        WHERE ii.interviewer.id = :interviewerId
    """)
    List<Interview> findByInterviewerId(@Param("interviewerId") UUID interviewerId);

    @Query(value = "SELECT DISTINCT ii.interview.id FROM InterviewInterviewer ii WHERE ii.interviewer.id = :interviewerId ORDER BY ii.interview.startTime DESC",
           countQuery = "SELECT COUNT(DISTINCT ii.interview.id) FROM InterviewInterviewer ii WHERE ii.interviewer.id = :interviewerId")
    Page<UUID> findIdsByInterviewerId(@Param("interviewerId") UUID interviewerId, Pageable pageable);

    @Query("""
        SELECT DISTINCT i
        FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.scheduledBy
        LEFT JOIN FETCH i.interviewers ii
        LEFT JOIN FETCH ii.interviewer
        WHERE i.status = :status
    """)
    List<Interview> findByStatus(@Param("status") InterviewStatus status);

    @Query(value = "SELECT i.id FROM Interview i WHERE i.status = :status ORDER BY i.startTime DESC",
           countQuery = "SELECT COUNT(i) FROM Interview i WHERE i.status = :status")
    Page<UUID> findIdsByStatus(@Param("status") InterviewStatus status, Pageable pageable);

    @Query("""
        SELECT DISTINCT i
        FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.scheduledBy
        LEFT JOIN FETCH i.interviewers ii
        LEFT JOIN FETCH ii.interviewer
        WHERE i.startTime >= :from AND i.startTime <= :to
    """)
    List<Interview> findByDateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT i.id FROM Interview i WHERE i.startTime >= :from AND i.startTime <= :to ORDER BY i.startTime DESC",
           countQuery = "SELECT COUNT(i) FROM Interview i WHERE i.startTime >= :from AND i.startTime <= :to")
    Page<UUID> findIdsByDateRange(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Interview i
        JOIN i.interviewers ii
        WHERE ii.interviewer.id = :interviewerId
        AND i.status NOT IN ('CANCELLED')
        AND i.startTime < :endTime
        AND i.endTime > :startTime
    """)
    boolean existsByInterviewerAndTimeRange(
            @Param("interviewerId") UUID interviewerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}

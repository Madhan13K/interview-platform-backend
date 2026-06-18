package com.interview_platform_backend.interview_platform_backend.jobposition.repository;

import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPositionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPosition, UUID> {

    List<JobPosition> findByStatus(JobPositionStatus status);

    Page<JobPosition> findByStatus(JobPositionStatus status, Pageable pageable);

    List<JobPosition> findByDepartment(String department);

    List<JobPosition> findByCreatedById(UUID userId);

    List<JobPosition> findByHiringManagerId(UUID managerId);

    @Query("SELECT jp FROM JobPosition jp WHERE jp.status = 'OPEN' ORDER BY jp.createdAt DESC")
    List<JobPosition> findAllOpen();

    @Query("SELECT jp FROM JobPosition jp LEFT JOIN FETCH jp.createdBy LEFT JOIN FETCH jp.hiringManager WHERE jp.id = :id")
    Optional<JobPosition> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT jp FROM JobPosition jp
        WHERE LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(jp.department) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(jp.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(jp.skills) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<JobPosition> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(JobPositionStatus status);
}


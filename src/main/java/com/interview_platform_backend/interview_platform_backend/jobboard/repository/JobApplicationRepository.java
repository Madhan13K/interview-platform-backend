package com.interview_platform_backend.interview_platform_backend.jobboard.repository;

import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationStatus;
import com.interview_platform_backend.interview_platform_backend.jobboard.entity.JobApplication;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    List<JobApplication> findByCandidate(User candidate);

    List<JobApplication> findByJobPosition(JobPosition jobPosition);

    List<JobApplication> findByCandidateIdOrderByAppliedAtDesc(UUID candidateId);

    List<JobApplication> findByJobPositionId(UUID jobPositionId);

    List<JobApplication> findByStatus(ApplicationStatus status);

    boolean existsByJobPositionIdAndCandidateId(UUID jobPositionId, UUID candidateId);

    long countByJobPositionIdAndStatus(UUID jobPositionId, ApplicationStatus status);
}

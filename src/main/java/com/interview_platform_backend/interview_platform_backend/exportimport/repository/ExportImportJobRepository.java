package com.interview_platform_backend.interview_platform_backend.exportimport.repository;

import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobStatus;
import com.interview_platform_backend.interview_platform_backend.exportimport.entity.ExportImportJob.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExportImportJobRepository extends JpaRepository<ExportImportJob, UUID> {

    Page<ExportImportJob> findByUserId(UUID userId, Pageable pageable);

    List<ExportImportJob> findByStatus(JobStatus status);

    List<ExportImportJob> findByUserIdAndType(UUID userId, JobType type);
}

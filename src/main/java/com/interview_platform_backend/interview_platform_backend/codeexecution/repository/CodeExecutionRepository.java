package com.interview_platform_backend.interview_platform_backend.codeexecution.repository;

import com.interview_platform_backend.interview_platform_backend.codeexecution.entity.CodeExecution;
import com.interview_platform_backend.interview_platform_backend.codeexecution.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeExecutionRepository extends JpaRepository<CodeExecution, UUID> {

    List<CodeExecution> findByCodingSessionIdOrderByCreatedAtDesc(UUID codingSessionId);

    List<CodeExecution> findByExecutedByIdOrderByCreatedAtDesc(UUID userId);

    List<CodeExecution> findByStatus(ExecutionStatus status);

    List<CodeExecution> findByStatusIn(List<ExecutionStatus> statuses);

    long countByCodingSessionId(UUID codingSessionId);
}

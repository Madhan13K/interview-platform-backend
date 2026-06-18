package com.interview_platform_backend.interview_platform_backend.codeeditor.repository;

import com.interview_platform_backend.interview_platform_backend.codeeditor.entity.CodingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodingSessionRepository extends JpaRepository<CodingSession, UUID> {

    List<CodingSession> findByInterviewId(UUID interviewId);

    Optional<CodingSession> findByInterviewIdAndEndedAtIsNull(UUID interviewId);
}


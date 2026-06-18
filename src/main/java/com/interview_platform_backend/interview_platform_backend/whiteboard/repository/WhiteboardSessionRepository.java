package com.interview_platform_backend.interview_platform_backend.whiteboard.repository;

import com.interview_platform_backend.interview_platform_backend.whiteboard.entity.WhiteboardSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WhiteboardSessionRepository extends JpaRepository<WhiteboardSession, UUID> {

    List<WhiteboardSession> findByInterviewId(UUID interviewId);

    List<WhiteboardSession> findByIsActiveTrue();

    List<WhiteboardSession> findByCreatedById(UUID createdById);
}

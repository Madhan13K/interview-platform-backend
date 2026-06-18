package com.interview_platform_backend.interview_platform_backend.whiteboard.repository;

import com.interview_platform_backend.interview_platform_backend.whiteboard.entity.WhiteboardStroke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WhiteboardStrokeRepository extends JpaRepository<WhiteboardStroke, UUID> {

    List<WhiteboardStroke> findBySessionIdOrderBySequenceNumberAsc(UUID sessionId);

    long countBySessionId(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}

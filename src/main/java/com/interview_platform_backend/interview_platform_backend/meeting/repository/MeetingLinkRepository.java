package com.interview_platform_backend.interview_platform_backend.meeting.repository;

import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeetingLinkRepository extends JpaRepository<MeetingLink, UUID> {

    Optional<MeetingLink> findByInterviewId(UUID interviewId);

    boolean existsByInterviewId(UUID interviewId);
}


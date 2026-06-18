package com.interview_platform_backend.interview_platform_backend.candidate.repository;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewInterviewer;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewInterviewerRepository extends JpaRepository<InterviewInterviewer, UUID> {
    List<InterviewInterviewer> findByInterview(Interview interview);
    Optional<InterviewInterviewer> findByInterviewAndInterviewer(Interview interview, User interviewer);
}

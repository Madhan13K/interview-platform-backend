package com.interview_platform_backend.interview_platform_backend.candidate.repository;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewFeedBack;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedBack, UUID> {

    List<InterviewFeedBack> findByInterview(Interview interview);

    Optional<InterviewFeedBack> findByInterviewAndInterviewer(Interview interview, User interviewer);

    boolean existsByInterviewAndInterviewer(Interview interview, User interviewer);

    List<InterviewFeedBack> findByInterviewer(User interviewer);
}

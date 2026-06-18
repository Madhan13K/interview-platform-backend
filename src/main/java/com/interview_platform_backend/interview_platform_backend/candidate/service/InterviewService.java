package com.interview_platform_backend.interview_platform_backend.candidate.service;


import com.interview_platform_backend.interview_platform_backend.candidate.dto.*;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InterviewService {
    InterviewResponse createInterview(CreateInterviewRequest request, UUID scheduledByUserId);

    InterviewResponse getInterview(UUID interviewId);

    List<InterviewResponse> getInterviews();

    PaginatedResponse<InterviewResponse> getInterviewsPaginated(int page, int size);

    InterviewResponse updateInterview(UUID interviewId, UpdateInterviewRequest request);

    InterviewResponse cancelInterview(UUID interviewId, CancelInterviewRequest request);

    InterviewResponse updateStatus(UUID interviewId, InterviewStatus status);

    void deleteInterview(UUID interviewId);

    // My interviews
    List<InterviewResponse> getMyInterviewsAsCandidate(UUID userId);

    PaginatedResponse<InterviewResponse> getMyInterviewsAsCandidatePaginated(UUID userId, int page, int size);

    List<InterviewResponse> getMyInterviewsAsInterviewer(UUID userId);

    PaginatedResponse<InterviewResponse> getMyInterviewsAsInterviewerPaginated(UUID userId, int page, int size);

    // Interviewer management
    InterviewResponse addInterviewer(UUID interviewId, UUID interviewerId, boolean isPrimary);

    InterviewResponse removeInterviewer(UUID interviewId, UUID interviewerId);

    // Feedback
    FeedbackResponse submitFeedback(UUID interviewId, UUID interviewerId, SubmitFeedbackRequest request);

    List<FeedbackResponse> getInterviewFeedback(UUID interviewId);

    FeedbackResponse getFeedbackByInterviewer(UUID interviewId, UUID interviewerId);

    List<FeedbackResponse> getAllFeedbackByInterviewer(UUID interviewerId);

    // Filters
    List<InterviewResponse> getInterviewsByStatus(InterviewStatus status);

    PaginatedResponse<InterviewResponse> getInterviewsByStatusPaginated(InterviewStatus status, int page, int size);

    List<InterviewResponse> getInterviewsByDateRange(Instant from, Instant to);

    PaginatedResponse<InterviewResponse> getInterviewsByDateRangePaginated(Instant from, Instant to, int page, int size);
}
package com.interview_platform_backend.interview_platform_backend.jobposition.service;

import com.interview_platform_backend.interview_platform_backend.jobposition.dto.*;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPositionStatus;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;

import java.util.List;
import java.util.UUID;

public interface JobPositionService {

    JobPositionResponse createJobPosition(CreateJobPositionRequest request, UUID createdBy);

    JobPositionResponse getJobPosition(UUID id);

    List<JobPositionResponse> getAllJobPositions();

    PaginatedResponse<JobPositionResponse> getJobPositionsPaginated(int page, int size);

    List<JobPositionResponse> getJobPositionsByStatus(JobPositionStatus status);

    PaginatedResponse<JobPositionResponse> searchJobPositions(String keyword, int page, int size);

    JobPositionResponse updateJobPosition(UUID id, UpdateJobPositionRequest request);

    JobPositionResponse updateStatus(UUID id, JobPositionStatus status);

    void deleteJobPosition(UUID id);

    JobPositionResponse linkInterviewToPosition(UUID positionId, UUID interviewId);

    JobPositionResponse unlinkInterviewFromPosition(UUID interviewId);

    List<JobPositionResponse> getMyJobPositions(UUID userId);
}


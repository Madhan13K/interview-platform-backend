package com.interview_platform_backend.interview_platform_backend.bulk.service;

import com.interview_platform_backend.interview_platform_backend.bulk.dto.*;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;

public interface BulkOperationService {

    BulkOperationResponse<InterviewResponse> bulkScheduleInterviews(BulkScheduleInterviewsRequest request, java.util.UUID scheduledByUserId);

    BulkOperationResponse<BulkInviteResult> bulkInviteCandidates(BulkInviteCandidatesRequest request);

    byte[] bulkExportData(BulkExportRequest request);
}


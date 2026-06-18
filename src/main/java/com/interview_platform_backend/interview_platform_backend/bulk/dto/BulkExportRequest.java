package com.interview_platform_backend.interview_platform_backend.bulk.dto;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkExportRequest {

    private ExportType exportType;

    private ExportFormat format;

    private InterviewStatus statusFilter;

    private Instant fromDate;

    private Instant toDate;

    public enum ExportType {
        INTERVIEWS,
        CANDIDATES,
        FEEDBACK,
        SCORECARDS
    }

    public enum ExportFormat {
        CSV,
        JSON
    }
}


package com.interview_platform_backend.interview_platform_backend.exportimport.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportImportJobResponse {

    private UUID id;
    private String type;
    private String format;
    private String status;
    private String entityType;
    private String fileName;
    private Integer totalRecords;
    private Integer processedRecords;
    private String errorMessage;
    private String downloadUrl;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Double progress;
}

package com.interview_platform_backend.interview_platform_backend.bulk.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResponse<T> {
    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<T> successResults;
    private List<BulkError> errors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkError {
        private int index;
        private String identifier;
        private String errorMessage;
    }
}


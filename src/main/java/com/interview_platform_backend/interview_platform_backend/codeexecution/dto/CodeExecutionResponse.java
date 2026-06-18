package com.interview_platform_backend.interview_platform_backend.codeexecution.dto;

import com.interview_platform_backend.interview_platform_backend.codeexecution.entity.ExecutionStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeExecutionResponse {

    private UUID id;
    private UUID codingSessionId;
    private UUID executedBy;
    private String language;
    private String sourceCode;
    private String stdin;
    private String stdout;
    private String stderr;
    private Integer exitCode;
    private ExecutionStatus status;
    private Long executionTimeMs;
    private Long memoryUsedBytes;
    private Long timeoutMs;
    private String errorMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
}

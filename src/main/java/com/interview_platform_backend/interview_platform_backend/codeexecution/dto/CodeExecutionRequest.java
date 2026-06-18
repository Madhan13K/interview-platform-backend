package com.interview_platform_backend.interview_platform_backend.codeexecution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeExecutionRequest {

    @NotNull(message = "Coding session ID is required")
    private UUID codingSessionId;

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Source code is required")
    private String sourceCode;

    /**
     * Optional standard input for the program.
     */
    private String stdin;

    /**
     * Timeout in milliseconds (default: 10000ms / 10s, max: 30000ms / 30s).
     */
    @Builder.Default
    private Long timeoutMs = 10000L;
}

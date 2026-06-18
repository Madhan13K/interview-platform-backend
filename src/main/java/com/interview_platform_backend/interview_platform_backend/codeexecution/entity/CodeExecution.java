package com.interview_platform_backend.interview_platform_backend.codeexecution.entity;

import com.interview_platform_backend.interview_platform_backend.codeeditor.entity.CodingSession;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coding_session_id", nullable = false)
    private CodingSession codingSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by", nullable = false)
    private User executedBy;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(name = "source_code", columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Column(name = "stdin", columnDefinition = "TEXT")
    private String stdin;

    @Column(name = "stdout", columnDefinition = "TEXT")
    private String stdout;

    @Column(name = "stderr", columnDefinition = "TEXT")
    private String stderr;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "memory_used_bytes")
    private Long memoryUsedBytes;

    @Column(name = "timeout_ms", nullable = false)
    @Builder.Default
    private Long timeoutMs = 10000L;

    @Column(name = "container_id", length = 80)
    private String containerId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

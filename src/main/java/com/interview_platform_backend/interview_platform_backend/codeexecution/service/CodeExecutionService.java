package com.interview_platform_backend.interview_platform_backend.codeexecution.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.interview_platform_backend.interview_platform_backend.codeeditor.entity.CodingSession;
import com.interview_platform_backend.interview_platform_backend.codeeditor.repository.CodingSessionRepository;
import com.interview_platform_backend.interview_platform_backend.codeexecution.config.CodeExecutionProperties;
import com.interview_platform_backend.interview_platform_backend.codeexecution.dto.CodeExecutionRequest;
import com.interview_platform_backend.interview_platform_backend.codeexecution.dto.CodeExecutionResponse;
import com.interview_platform_backend.interview_platform_backend.codeexecution.entity.CodeExecution;
import com.interview_platform_backend.interview_platform_backend.codeexecution.entity.ExecutionStatus;
import com.interview_platform_backend.interview_platform_backend.codeexecution.entity.SupportedLanguage;
import com.interview_platform_backend.interview_platform_backend.codeexecution.repository.CodeExecutionRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);

    private final DockerClient dockerClient;
    private final CodeExecutionRepository codeExecutionRepository;
    private final CodingSessionRepository codingSessionRepository;
    private final UserRepository userRepository;
    private final CodeExecutionProperties properties;
    private final AtomicInteger activeExecutions = new AtomicInteger(0);

    public CodeExecutionService(DockerClient dockerClient,
                                CodeExecutionRepository codeExecutionRepository,
                                CodingSessionRepository codingSessionRepository,
                                UserRepository userRepository,
                                CodeExecutionProperties properties) {
        this.dockerClient = dockerClient;
        this.codeExecutionRepository = codeExecutionRepository;
        this.codingSessionRepository = codingSessionRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    /**
     * Startup cleanup: remove orphaned containers from previous crashes.
     * Runs automatically when the application starts.
     */
    @jakarta.annotation.PostConstruct
    public void cleanupOrphanedContainers() {
        if (!properties.isEnabled()) return;

        try {
            log.info("Cleaning up orphaned code execution containers...");
            var containers = dockerClient.listContainersCmd()
                    .withLabelFilter(java.util.Map.of("interview-platform", "code-execution"))
                    .withShowAll(true)
                    .exec();

            int cleaned = 0;
            for (var container : containers) {
                try {
                    dockerClient.removeContainerCmd(container.getId())
                            .withForce(true)
                            .withRemoveVolumes(true)
                            .exec();
                    cleaned++;
                } catch (Exception e) {
                    log.warn("Failed to remove orphaned container {}: {}", container.getId(), e.getMessage());
                }
            }

            if (cleaned > 0) {
                log.info("Cleaned up {} orphaned code execution containers", cleaned);
            }

            // Also mark any RUNNING/QUEUED executions as ERROR (stale from crash)
            codeExecutionRepository.findByStatusIn(
                    List.of(ExecutionStatus.RUNNING, ExecutionStatus.QUEUED)
            ).forEach(exec -> {
                exec.setStatus(ExecutionStatus.ERROR);
                exec.setErrorMessage("Execution interrupted by server restart");
                exec.setCompletedAt(Instant.now());
                codeExecutionRepository.save(exec);
            });
        } catch (Exception e) {
            log.warn("Container cleanup on startup failed (Docker may be unavailable): {}", e.getMessage());
        }
    }

    /**
     * Submit code for execution in a Docker sandbox.
     */
    @Transactional
    public CodeExecutionResponse submitExecution(CodeExecutionRequest request, String userEmail) {
        if (!properties.isEnabled()) {
            throw new BadRequestException("Code execution is currently disabled");
        }

        // Validate source code size
        if (request.getSourceCode().length() > properties.getMaxSourceCodeSize()) {
            throw new BadRequestException("Source code exceeds maximum size of " + properties.getMaxSourceCodeSize() + " characters");
        }

        // Validate language
        SupportedLanguage language = SupportedLanguage.fromId(request.getLanguage());

        // Validate timeout
        long timeout = request.getTimeoutMs() != null ? request.getTimeoutMs() : properties.getDefaultTimeoutMs();
        if (timeout > properties.getMaxTimeoutMs()) {
            timeout = properties.getMaxTimeoutMs();
        }

        // Validate concurrent executions
        if (activeExecutions.get() >= properties.getMaxConcurrentExecutions()) {
            throw new BadRequestException("Maximum concurrent executions reached. Please try again later.");
        }

        // Find coding session
        CodingSession codingSession = codingSessionRepository.findById(request.getCodingSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("CodingSession", "id", request.getCodingSessionId()));

        // Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        // Create execution record
        CodeExecution execution = CodeExecution.builder()
                .codingSession(codingSession)
                .executedBy(user)
                .language(language.getLanguageId())
                .sourceCode(request.getSourceCode())
                .stdin(request.getStdin())
                .timeoutMs(timeout)
                .status(ExecutionStatus.QUEUED)
                .build();

        execution = codeExecutionRepository.save(execution);

        // Execute asynchronously
        executeInSandbox(execution.getId());

        return toResponse(execution);
    }

    /**
     * Execute code inside a Docker container with strict resource limits.
     */
    @Async
    @Transactional
    public void executeInSandbox(UUID executionId) {
        CodeExecution execution = codeExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("CodeExecution", "id", executionId));

        activeExecutions.incrementAndGet();
        String containerId = null;

        try {
            SupportedLanguage language = SupportedLanguage.fromId(execution.getLanguage());

            execution.setStatus(ExecutionStatus.RUNNING);
            execution.setStartedAt(Instant.now());
            codeExecutionRepository.save(execution);

            // Create container with strict security constraints
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(properties.getMemoryLimitBytes())
                    .withMemorySwap(properties.getMemoryLimitBytes()) // No swap
                    .withCpuPeriod(properties.getCpuPeriodMicros())
                    .withCpuQuota(properties.getCpuQuotaMicros())
                    .withPidsLimit(properties.getPidsLimit())
                    .withNetworkMode(properties.isNetworkDisabled() ? "none" : "bridge")
                    .withReadonlyRootfs(false)
                    .withSecurityOpts(List.of("no-new-privileges"))
                    .withCapDrop(Capability.ALL);

            CreateContainerResponse container = dockerClient.createContainerCmd(language.getDockerImage())
                    .withHostConfig(hostConfig)
                    .withNetworkDisabled(properties.isNetworkDisabled())
                    .withCmd(language.getExecuteCommand())
                    .withWorkingDir("/tmp/code")
                    .withUser("nobody")
                    .withAttachStdin(execution.getStdin() != null)
                    .withStdinOpen(execution.getStdin() != null)
                    .withLabels(java.util.Map.of(
                            "interview-platform", "code-execution",
                            "execution-id", executionId.toString()
                    ))
                    .exec();

            containerId = container.getId();
            execution.setContainerId(containerId);
            codeExecutionRepository.save(execution);

            // Copy source code into container
            String sourceCode = execution.getSourceCode();
            byte[] tarBytes = createTarArchive(language.getFileName(), sourceCode.getBytes(StandardCharsets.UTF_8));
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(new ByteArrayInputStream(tarBytes))
                    .withRemotePath("/tmp/code")
                    .exec();

            // Start container
            dockerClient.startContainerCmd(containerId).exec();

            // Provide stdin if specified
            if (execution.getStdin() != null && !execution.getStdin().isEmpty()) {
                try {
                    dockerClient.attachContainerCmd(containerId)
                            .withStdIn(new ByteArrayInputStream(
                                    execution.getStdin().getBytes(StandardCharsets.UTF_8)))
                            .withFollowStream(false)
                            .exec(new ResultCallback.Adapter<>());
                } catch (Exception e) {
                    log.debug("Could not attach stdin: {}", e.getMessage());
                }
            }

            // Wait for container to finish with timeout
            int exitCode;
            try {
                WaitContainerResultCallback waitCallback = dockerClient
                        .waitContainerCmd(containerId)
                        .exec(new WaitContainerResultCallback());

                boolean completed = waitCallback.awaitCompletion(execution.getTimeoutMs(), TimeUnit.MILLISECONDS);

                if (!completed) {
                    // Timeout - kill the container
                    dockerClient.killContainerCmd(containerId).exec();
                    execution.setStatus(ExecutionStatus.TIMEOUT);
                    execution.setErrorMessage("Execution timed out after " + execution.getTimeoutMs() + "ms");
                    execution.setCompletedAt(Instant.now());
                    execution.setExecutionTimeMs(execution.getTimeoutMs());
                    codeExecutionRepository.save(execution);
                    return;
                }

                exitCode = waitCallback.awaitStatusCode();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                execution.setStatus(ExecutionStatus.ERROR);
                execution.setErrorMessage("Execution was interrupted");
                execution.setCompletedAt(Instant.now());
                codeExecutionRepository.save(execution);
                return;
            }

            // Collect stdout and stderr
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(false)
                    .withFollowStream(false)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (stdout.length() < properties.getMaxOutputSize()) {
                                stdout.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                            }
                        }
                    }).awaitCompletion(5, TimeUnit.SECONDS);

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(false)
                    .withStdErr(true)
                    .withFollowStream(false)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (stderr.length() < properties.getMaxOutputSize()) {
                                stderr.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                            }
                        }
                    }).awaitCompletion(5, TimeUnit.SECONDS);

            // Get memory usage from container inspection
            try {
                InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
                if (inspectResponse.getState() != null && inspectResponse.getState().getOOMKilled() != null
                        && inspectResponse.getState().getOOMKilled()) {
                    execution.setStatus(ExecutionStatus.MEMORY_LIMIT_EXCEEDED);
                    execution.setErrorMessage("Process killed: memory limit exceeded ("
                            + (properties.getMemoryLimitBytes() / 1024 / 1024) + "MB)");
                }
            } catch (Exception e) {
                log.debug("Could not inspect container: {}", e.getMessage());
            }

            // Update execution record
            execution.setStdout(truncate(stdout.toString(), properties.getMaxOutputSize()));
            execution.setStderr(truncate(stderr.toString(), properties.getMaxOutputSize()));
            execution.setExitCode(exitCode);
            if (execution.getStatus() == ExecutionStatus.RUNNING) {
                execution.setStatus(ExecutionStatus.COMPLETED);
            }
            execution.setCompletedAt(Instant.now());
            execution.setExecutionTimeMs(
                    execution.getCompletedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli());

            codeExecutionRepository.save(execution);

        } catch (Exception e) {
            log.error("Code execution failed for execution {}: {}", executionId, e.getMessage(), e);
            execution.setStatus(ExecutionStatus.ERROR);
            execution.setErrorMessage("Internal execution error: " + e.getMessage());
            execution.setCompletedAt(Instant.now());
            if (execution.getStartedAt() != null) {
                execution.setExecutionTimeMs(
                        execution.getCompletedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli());
            }
            codeExecutionRepository.save(execution);
        } finally {
            activeExecutions.decrementAndGet();
            // Cleanup: remove the container
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId)
                            .withForce(true)
                            .withRemoveVolumes(true)
                            .exec();
                } catch (Exception e) {
                    log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
                }
            }
        }
    }

    /**
     * Get execution result by ID.
     */
    @Transactional(readOnly = true)
    public CodeExecutionResponse getExecution(UUID executionId) {
        CodeExecution execution = codeExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("CodeExecution", "id", executionId));
        return toResponse(execution);
    }

    /**
     * Get all executions for a coding session.
     */
    @Transactional(readOnly = true)
    public List<CodeExecutionResponse> getExecutionsForSession(UUID codingSessionId) {
        return codeExecutionRepository.findByCodingSessionIdOrderByCreatedAtDesc(codingSessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get supported languages list.
     */
    public List<String> getSupportedLanguages() {
        return java.util.Arrays.stream(SupportedLanguage.values())
                .map(SupportedLanguage::getLanguageId)
                .toList();
    }

    /**
     * Create a minimal TAR archive containing a single file.
     * TAR format: 512-byte header + file content padded to 512-byte blocks + 1024 zero bytes end marker.
     */
    private byte[] createTarArchive(String fileName, byte[] content) {
        int contentLength = content.length;
        int headerSize = 512;
        int contentBlocks = (contentLength + 511) / 512;
        int totalSize = headerSize + (contentBlocks * 512) + 1024;

        byte[] tar = new byte[totalSize];

        // File name (offset 0, max 100 bytes)
        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, tar, 0, Math.min(nameBytes.length, 100));

        // File mode (offset 100, 8 bytes) - 0644
        System.arraycopy("0000644\0".getBytes(), 0, tar, 100, 8);

        // Owner UID (offset 108, 8 bytes) - 65534 (nobody)
        System.arraycopy("0177776\0".getBytes(), 0, tar, 108, 8);

        // Group GID (offset 116, 8 bytes) - 65534 (nogroup)
        System.arraycopy("0177776\0".getBytes(), 0, tar, 116, 8);

        // File size in octal (offset 124, 12 bytes)
        String sizeOctal = String.format("%011o", contentLength);
        System.arraycopy((sizeOctal + "\0").getBytes(), 0, tar, 124, 12);

        // Modification time (offset 136, 12 bytes)
        String mtime = String.format("%011o", System.currentTimeMillis() / 1000);
        System.arraycopy((mtime + "\0").getBytes(), 0, tar, 136, 12);

        // Type flag (offset 156) - '0' for regular file
        tar[156] = '0';

        // USTAR magic (offset 257)
        System.arraycopy("ustar\000".getBytes(), 0, tar, 257, 6);

        // Version (offset 263)
        System.arraycopy("00".getBytes(), 0, tar, 263, 2);

        // Calculate and set checksum (offset 148, 8 bytes)
        // First set checksum field to spaces
        java.util.Arrays.fill(tar, 148, 156, (byte) ' ');
        int checksum = 0;
        for (int i = 0; i < headerSize; i++) {
            checksum += (tar[i] & 0xFF);
        }
        String checksumOctal = String.format("%06o\0 ", checksum);
        System.arraycopy(checksumOctal.getBytes(), 0, tar, 148, 8);

        // Copy file content after header
        System.arraycopy(content, 0, tar, headerSize, contentLength);

        return tar;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "\n... [output truncated]";
    }

    private CodeExecutionResponse toResponse(CodeExecution execution) {
        return CodeExecutionResponse.builder()
                .id(execution.getId())
                .codingSessionId(execution.getCodingSession().getId())
                .executedBy(execution.getExecutedBy().getId())
                .language(execution.getLanguage())
                .sourceCode(execution.getSourceCode())
                .stdin(execution.getStdin())
                .stdout(execution.getStdout())
                .stderr(execution.getStderr())
                .exitCode(execution.getExitCode())
                .status(execution.getStatus())
                .executionTimeMs(execution.getExecutionTimeMs())
                .memoryUsedBytes(execution.getMemoryUsedBytes())
                .timeoutMs(execution.getTimeoutMs())
                .errorMessage(execution.getErrorMessage())
                .createdAt(execution.getCreatedAt())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .build();
    }
}

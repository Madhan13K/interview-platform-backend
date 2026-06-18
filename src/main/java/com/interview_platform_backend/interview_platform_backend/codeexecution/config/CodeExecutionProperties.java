package com.interview_platform_backend.interview_platform_backend.codeexecution.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "code-execution")
@Getter
@Setter
public class CodeExecutionProperties {

    /**
     * Maximum execution timeout in milliseconds. User cannot exceed this.
     */
    private long maxTimeoutMs = 30000;

    /**
     * Default execution timeout in milliseconds.
     */
    private long defaultTimeoutMs = 10000;

    /**
     * Memory limit per container in bytes (default: 256MB).
     */
    private long memoryLimitBytes = 256 * 1024 * 1024L;

    /**
     * CPU period in microseconds for CPU quota.
     */
    private long cpuPeriodMicros = 100000;

    /**
     * CPU quota in microseconds (50000 = 50% of one core).
     */
    private long cpuQuotaMicros = 50000;

    /**
     * Maximum number of processes inside the container (PID limit).
     */
    private long pidsLimit = 64;

    /**
     * Maximum output size in characters to prevent memory issues.
     */
    private int maxOutputSize = 65536;

    /**
     * Whether to disable network access in execution containers.
     */
    private boolean networkDisabled = true;

    /**
     * Whether code execution feature is enabled.
     */
    private boolean enabled = true;

    /**
     * Maximum concurrent executions allowed globally.
     */
    private int maxConcurrentExecutions = 10;

    /**
     * Maximum source code size in characters.
     */
    private int maxSourceCodeSize = 100000;
}

package com.interview_platform_backend.interview_platform_backend.config.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the async thread pool with MDC propagation support.
 * This ensures correlation IDs flow through to @Async methods.
 */
@Configuration
@EnableAsync
public class AsyncMdcConfig {

    private final MdcTaskDecorator mdcTaskDecorator;

    public AsyncMdcConfig(MdcTaskDecorator mdcTaskDecorator) {
        this.mdcTaskDecorator = mdcTaskDecorator;
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-mdc-");
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.initialize();
        return executor;
    }
}

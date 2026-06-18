package com.interview_platform_backend.interview_platform_backend.config.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Task decorator that propagates MDC (correlation IDs) from the calling thread
 * to async worker threads.
 * 
 * Without this, log entries from @Async methods and CompletableFuture tasks
 * would lose the correlation context.
 * 
 * Applied automatically to the Spring task executor via AsyncConfig.
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture MDC context from the calling thread
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // Set MDC context in the worker thread
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                // Clear MDC to prevent leaking
                MDC.clear();
            }
        };
    }
}

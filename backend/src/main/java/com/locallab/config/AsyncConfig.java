package com.locallab.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous task execution.
 *
 * <p>This configuration enables Spring's async processing capability, which is required for
 * executing experiments in background threads. The executor is configured with sensible defaults
 * for local LLM experimentation workloads.
 *
 * <h3>Executor Configuration:</h3>
 *
 * <ul>
 *   <li><strong>Core Pool Size:</strong> 2 threads - experiments are resource-intensive
 *   <li><strong>Max Pool Size:</strong> 4 threads - allows some concurrency whilst respecting local
 *       resources
 *   <li><strong>Queue Capacity:</strong> 10 - limits pending experiments
 *   <li><strong>Thread Name Prefix:</strong> "experiment-exec-" for easy identification in logs
 * </ul>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentExecutorService
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates the task executor for async experiment execution.
     *
     * <p>The executor is configured conservatively since LLM inference is resource-intensive and
     * typically runs on local hardware. This prevents overloading the system with too many
     * concurrent experiments.
     *
     * @return the configured task executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("experiment-exec-");
        executor.initialize();
        return executor;
    }
}

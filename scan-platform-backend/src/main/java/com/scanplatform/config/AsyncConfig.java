package com.scanplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池，用于 WebHook 触发的 Shell 扫描，避免阻塞 HTTP 线程。
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "scanTaskExecutor")
    public Executor scanTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 批量手动扫描会并发提交多条异步任务，过小会导致队列堆积、任务长时间排队
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("scan-async-");
        executor.initialize();
        return executor;
    }
}

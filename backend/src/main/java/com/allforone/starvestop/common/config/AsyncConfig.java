package com.allforone.starvestop.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    private final int FCM_THREAD_POOL_SIZE = 100;

    @Bean(name = "fcmThreadPool")
    public Executor fcmThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(FCM_THREAD_POOL_SIZE);
        executor.setMaxPoolSize(FCM_THREAD_POOL_SIZE);

        executor.setQueueCapacity(FCM_THREAD_POOL_SIZE * 5);

        // 🚀 [핵심 배압 제어] 큐가 꽉 차면 지시를 내린 메인 스레드가 직접 처리하게 만듦 (OOM 방어)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setThreadNamePrefix("FCM-Async-");
        executor.initialize();
        return executor;
    }
}

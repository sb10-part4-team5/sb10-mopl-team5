package com.springboot.datagenerator.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadConfig {

    @Bean(name = "jdbcWorker")
    public Executor jdbcWorker() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("jdbc-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 큐가 꽉 찼을 때 작업이 들어오면 main 스레드가 대신 실행하도록 하여 유실 방지
        executor.setWaitForTasksToCompleteOnShutdown(true); // 앱 종료 시 실행중인 작업이 종료될 때 까지 대기
        executor.setAwaitTerminationSeconds(60); // 60초 동안 작업이 끝나지 않으면 강제 종료
        executor.initialize();
        return executor;
    }
}

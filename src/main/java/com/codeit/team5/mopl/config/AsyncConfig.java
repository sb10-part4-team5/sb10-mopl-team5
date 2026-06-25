package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.global.async.CompositeTaskDecorator;
import com.codeit.team5.mopl.global.async.MdcTaskDecorator;
import com.codeit.team5.mopl.global.async.SecurityContextTaskDecorator;
import java.util.List;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "binaryContentUploadExecutor")
    public Executor binaryContentUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("binary-upload-");
        executor.setTaskDecorator(new CompositeTaskDecorator(
                List.of(new MdcTaskDecorator(), new SecurityContextTaskDecorator())));
        executor.initialize();
        return executor;
    }
}

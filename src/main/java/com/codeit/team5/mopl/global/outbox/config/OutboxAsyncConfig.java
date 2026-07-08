package com.codeit.team5.mopl.global.outbox.config;

import java.util.concurrent.Executor;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import com.codeit.team5.mopl.global.async.MdcTaskDecorator;

@EnableAsync
@Configuration
public class OutboxAsyncConfig {

    @Bean("outboxEventWorker")
    public Executor outboxEventWorker(ThreadPoolTaskExecutorBuilder builder) {
        return builder.corePoolSize(10).maxPoolSize(20).queueCapacity(50)
                .taskDecorator(new MdcTaskDecorator())
                .threadNamePrefix("outbox-event-").build();
    }
}

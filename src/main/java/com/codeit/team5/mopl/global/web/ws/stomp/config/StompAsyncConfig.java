package com.codeit.team5.mopl.global.web.ws.stomp.config;

import com.codeit.team5.mopl.global.async.MdcTaskDecorator;
import java.util.concurrent.Executor;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
class StompAsyncConfig {

    @Bean("stompWorker")
    public Executor stompWorker(ThreadPoolTaskExecutorBuilder builder) {
        return builder.corePoolSize(10)
                .maxPoolSize(30)
                .queueCapacity(20)
                .threadNamePrefix("stomp-Async-")
                .taskDecorator(new MdcTaskDecorator())
                .build();
    }
}

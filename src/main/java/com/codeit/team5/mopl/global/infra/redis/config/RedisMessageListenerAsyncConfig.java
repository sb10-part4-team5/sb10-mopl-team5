package com.codeit.team5.mopl.global.infra.redis.config;

import com.codeit.team5.mopl.global.async.MdcTaskDecorator;
import java.util.concurrent.Executor;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class RedisMessageListenerAsyncConfig {

    @Bean("redisMessageWorker")
    public Executor redisMessageWorker(ThreadPoolTaskExecutorBuilder builder) {
        return builder.corePoolSize(10)
                .maxPoolSize(30)
                .queueCapacity(20)
                .threadNamePrefix("redis-message-listener-async-")
                .taskDecorator(new MdcTaskDecorator())
                .build();
    }
}

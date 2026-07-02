package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.global.async.MdcTaskDecorator;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 10;

    /**
     * 배치 Job을 비동기로 실행하기 위한 스레드 풀.
     * <p>
     * 동시성 상한을 두어 수집 요청이 몰려도 서버 자원이 무한정 잠식되지 않도록 한다.
     * 큐까지 가득 차면 AbortPolicy로 요청을 거부하며, 이때 발생하는 TaskRejectedException은
     * GlobalExceptionHandler에서 503으로 매핑된다.
     * <p>
     * MDC(requestId 등)만 복사한다. 콘텐츠 수집은 스케줄러로도 실행되는 시스템 작업이라
     * 인증 컨텍스트에 의존하지 않으므로 SecurityContext는 복사하지 않는다.
     */
    @Bean
    public ThreadPoolTaskExecutor batchJobTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("batch-job-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}

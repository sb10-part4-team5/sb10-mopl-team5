package com.codeit.team5.mopl.content.batch.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;

/**
 * skip(Exception.class)로 조용히 넘어가는 항목이 무엇이었고 왜 스킵됐는지 로그로 남긴다.
 * 그대로 두면 어떤 데이터가 유실됐는지 운영 중에 확인할 방법이 없다.
 */
@Slf4j
public class LoggingSkipListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[Batch] read 단계 스킵 - error={}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("[Batch] process 단계 스킵 - item={}, error={}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.warn("[Batch] write 단계 스킵 - item={}, error={}", item, t.getMessage());
    }
}

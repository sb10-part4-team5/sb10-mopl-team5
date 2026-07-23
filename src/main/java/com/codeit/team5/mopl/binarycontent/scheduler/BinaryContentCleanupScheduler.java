package com.codeit.team5.mopl.binarycontent.scheduler;

import com.codeit.team5.mopl.binarycontent.service.BinaryContentCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DELETED 상태의 BinaryContent를 하루 한 번 일괄 정리한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentCleanupScheduler {

    // 실행당 처리 상한. 대상 전체를 한 번에 로딩하지 않도록 청크 크기를 제한한다.
    private static final int CHUNK_SIZE = 500;

    private final BinaryContentCleanupService binaryContentCleanupService;

    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "binaryContentCleanup", lockAtMostFor = "30m", lockAtLeastFor = "1m")
    public void cleanUpDeleted() {
        int deleted = binaryContentCleanupService.cleanUp(CHUNK_SIZE);
        if (deleted > 0) {
            log.info("DELETED BinaryContent 정리 완료: 삭제 {}건", deleted);
        }
    }
}

package com.codeit.team5.mopl.content.scheduler;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.content.repository.SyncCursorRepository;
import com.codeit.team5.mopl.content.repository.opensearch.ContentDocumentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 콘텐츠 통계(watcherCount/averageRating/reviewCount)를 검색 인덱스에 주기적으로 반영한다.
 *
 * <p>시청 세션 참여/이탈, 리뷰 CUD로 통계는 매우 자주 바뀐다. 그때마다 검색 인덱스에 쓰면 대부분의
 * 쓰기가 다음 갱신에 바로 덮여 낭비되고, 검색 엔진 장애가 시청 세션 트랜잭션까지 번진다. 그래서 통계
 * 변경 시점에는 {@code content_stats.updated_at}만 갱신해두고, 여기서 주기적으로 모아 반영한다.
 * 그 대가로 검색 결과의 통계 값은 이 주기만큼 지연될 수 있다.</p>
 *
 * <p>"지난번에 어디까지 반영했는지"는 {@link SyncCursorRepository}가 DB에 커서로 저장한다. 인스턴스
 * 메모리가 아닌 DB에 두는 이유는, ShedLock으로 매 주기 실행 인스턴스가 바뀔 수 있어서다 — 커서가
 * 인스턴스 로컬에 있으면 오랜만에 락을 잡은 인스턴스가 처음부터(EPOCH부터) 다시 훑게 된다. 통계만
 * 갱신하는 게 아니라 문서 전체를 다시 만들어 덮어쓰므로, 제목·태그 같은 정적 필드가 이벤트 유실로
 * 어긋나 있었다면 이때 함께 교정된다.</p>
 *
 * <p>PostgreSQL의 {@code CURRENT_TIMESTAMP}는 트랜잭션 시작 시각을 반환한다. 그래서 실행 시간이 긴
 * 트랜잭션이 커밋되기 전에 이 스케줄러가 기준 시각을 "지금"까지 당겨버리면, 뒤늦게 커밋된 그 트랜잭션의
 * 변경분은 {@code updated_at > 커서} 조건에서 영구히 걸러진다. 그래서 기준 시각을 실제 조회 시각보다
 * 30초 뒤처지게 잡는다. ES 색인은 멱등이라 겹쳐 읽어도 무해하다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSearchSyncScheduler {

    private static final String CURSOR_NAME = "contentSearchSync";

    private final ContentStatsRepository contentStatsRepository;
    private final ContentRepository contentRepository;
    private final ContentDocumentRepository contentDocumentRepository;
    private final ContentMapper contentMapper;
    private final SyncCursorRepository syncCursorRepository;


    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @SchedulerLock(name = "contentSearchSync", lockAtMostFor = "10m", lockAtLeastFor = "4m")
    public void syncUpdatedStats() {
        Instant lastSyncedAt = syncCursorRepository.findSyncedAt(CURSOR_NAME);
        // 조회 이전 시각을 기준으로 잡아야, 조회하는 동안 들어온 변경을 다음 주기가 다시 가져간다.
        Instant syncStartedAt = Instant.now().minus(Duration.ofSeconds(30));

        List<UUID> updatedIds = contentStatsRepository.findIdsUpdatedAfter(lastSyncedAt);
        if (updatedIds.isEmpty()) {
            syncCursorRepository.updateSyncedAt(CURSOR_NAME, syncStartedAt);
            return;
        }

        List<Content> contents = contentRepository.findAllWithStatsAndTagsByIdIn(updatedIds);
        if (!contents.isEmpty()) {
            List<ContentDocument> documents = contents.stream()
                    .map(contentMapper::toDocument)
                    .toList();
            contentDocumentRepository.saveAll(documents);
        }

        // 색인에 성공했을 때만 커서를 옮긴다. 예외가 나면 다음 주기가 같은 구간을 다시 처리한다.
        syncCursorRepository.updateSyncedAt(CURSOR_NAME, syncStartedAt);
    }
}

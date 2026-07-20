package com.codeit.team5.mopl.content.scheduler;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
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
 * <p>"지난번에 어디까지 반영했는지"를 {@link #lastSyncedAt}에 기억해두고, 매번 그 이후에 바뀐 것만
 * 골라 처리한다. 통계만 갱신하는 게 아니라 문서 전체를 다시 만들어 덮어쓰므로, 제목·태그 같은 정적
 * 필드가 이벤트 유실로 어긋나 있었다면 이때 함께 교정된다.</p>
 *
 * <p>다만 이 기준 시각은 메모리에만 있어서 재기동하면 초기화된다. 그러면 전체 콘텐츠를 다시 색인하는데,
 * 콘텐츠가 수천 건 규모라 bulk 요청 한 번이면 끝나 부담이 없고 오히려 그동안 쌓인 불일치를 한 번에
 * 정리해준다. 단, 재기동 전까지는 교정이 일어나지 않고 DB에서 삭제됐는데 인덱스에만 남은 문서는
 * 지우지 못하므로, 완전한 정합성 보장 수단은 아니다.</p>
 *
 * <p>PostgreSQL의 {@code CURRENT_TIMESTAMP}는 트랜잭션 시작 시각을 반환한다. 그래서 실행 시간이 긴
 * 트랜잭션이 커밋되기 전에 이 스케줄러가 기준 시각을 "지금"까지 당겨버리면, 뒤늦게 커밋된 그 트랜잭션의
 * 변경분은 {@code updated_at > lastSyncedAt} 조건에서 영구히 걸러진다. 그래서 기준 시각을 실제 조회
 * 시각보다 30초 뒤처지게 잡는다. ES 색인은 멱등이라 겹쳐 읽어도 무해하다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSearchSyncScheduler {

    private final ContentStatsRepository contentStatsRepository;
    private final ContentRepository contentRepository;
    private final ContentDocumentRepository contentDocumentRepository;
    private final ContentMapper contentMapper;

    private Instant lastSyncedAt = Instant.EPOCH;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @SchedulerLock(name = "contentSearchSync", lockAtMostFor = "10m")
    public void syncUpdatedStats() {
        // 조회 이전 시각을 기준으로 잡아야, 조회하는 동안 들어온 변경을 다음 주기가 다시 가져간다.
        Instant syncStartedAt = Instant.now().minus(Duration.ofSeconds(30));

        List<UUID> updatedIds = contentStatsRepository.findIdsUpdatedAfter(lastSyncedAt);
        if (updatedIds.isEmpty()) {
            lastSyncedAt = syncStartedAt;
            return;
        }

        List<Content> contents = contentRepository.findAllWithStatsAndTagsByIdIn(updatedIds);
        if (!contents.isEmpty()) {
            List<ContentDocument> documents = contents.stream()
                    .map(contentMapper::toDocument)
                    .toList();
            contentDocumentRepository.saveAll(documents);
        }

        // 색인에 성공했을 때만 기준 시각을 옮긴다. 예외가 나면 다음 주기가 같은 구간을 다시 처리한다.
        lastSyncedAt = syncStartedAt;
        log.debug("검색 인덱스 통계 동기화 완료: {}건", contents.size());
    }
}

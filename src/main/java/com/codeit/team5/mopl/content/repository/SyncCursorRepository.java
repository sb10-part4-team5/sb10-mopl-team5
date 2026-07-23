package com.codeit.team5.mopl.content.repository;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 콘텐츠 검색 동기화 스케줄러가 "어디까지 처리했는지"를 인스턴스 메모리가 아닌 Redis에 커서로 저장한다.
 * ShedLock으로 매 주기 실행 인스턴스가 바뀔 수 있어, 커서도 인스턴스가 아닌 공유 저장소에 둬야
 * 특정 인스턴스가 오랜만에 락을 잡았을 때 처음부터 다시 훑는 걸 막을 수 있다.
 *
 * <p>커서는 만료되면 안 되는 상태이므로 캐시 키와 달리 <b>TTL을 걸지 않는다</b>. Redis 디스크
 * 영속화(RDB/AOF)에 의존하며, 재기동 등으로 커서가 유실되더라도 스케줄러가 조금 더 넓은 구간을
 * 다시 색인할 뿐 결과는 멱등하게 수렴한다.</p>
 */
@Repository
@RequiredArgsConstructor
public class SyncCursorRepository {

    private static final String KEY = "mopl:content:sync-cursor:contentSearchSync";

    private final StringRedisTemplate redisTemplate;

    public Instant findSyncedAt() {
        String value = redisTemplate.opsForValue().get(KEY);
        return value == null ? Instant.EPOCH : Instant.parse(value);
    }

    public void updateSyncedAt(Instant syncedAt) {
        redisTemplate.opsForValue().set(KEY, syncedAt.toString());
    }
}

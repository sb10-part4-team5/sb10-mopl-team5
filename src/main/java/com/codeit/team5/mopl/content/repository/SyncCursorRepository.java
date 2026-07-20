package com.codeit.team5.mopl.content.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 스케줄러가 "어디까지 처리했는지"를 인스턴스 메모리가 아닌 DB에 커서로 저장한다.
 * ShedLock으로 매 주기 실행 인스턴스가 바뀔 수 있어, 커서도 인스턴스가 아닌 DB에 공유해야
 * 특정 인스턴스가 오랜만에 락을 잡았을 때 처음부터 다시 훑는 걸 막을 수 있다.
 */
@Repository
@RequiredArgsConstructor
public class SyncCursorRepository {

    private final JdbcTemplate jdbcTemplate;

    public Instant findSyncedAt(String name) {
        List<Instant> rows = jdbcTemplate.query(
                "SELECT synced_at FROM sync_cursor WHERE name = ?",
                (rs, rowNum) -> rs.getTimestamp("synced_at").toInstant(),
                name);
        return rows.isEmpty() ? Instant.EPOCH : rows.get(0);
    }

    public void updateSyncedAt(String name, Instant syncedAt) {
        jdbcTemplate.update(
                "INSERT INTO sync_cursor (name, synced_at) VALUES (?, ?) "
                        + "ON CONFLICT (name) DO UPDATE SET synced_at = EXCLUDED.synced_at",
                name, Timestamp.from(syncedAt));
    }
}

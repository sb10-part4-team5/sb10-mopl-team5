package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConversationGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO conversations (id, participant1, participant2, created_at) VALUES (?,?,?,?)";

    public ConversationGenerator(GeneratorProperties properties, JdbcTemplate template,
                                 @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    // Conversation은 ck_conv_order(participant1 < participant2) 체크 제약과
    // (participant1, participant2) 유니크 제약이 있어, Follow처럼 단순 랜덤 페어로는 안 되고
    // 정렬 + 중복 제거가 필요하다. DirectMessageGenerator가 참여자를 알아야 해서
    // conversationId -> [participant1, participant2] 매핑을 반환한다.
    public Map<UUID, UUID[]> run(List<UUID> userIds) {
        log.info("Generating conversations ({} users × {} each)...", userIds.size(), properties.conversationPerUser());

        record UuidPair(UUID p1, UUID p2) {}
        Set<UuidPair> seenPairs = new HashSet<>();
        List<UUID[]> pairs = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            UUID user = userIds.get(i);
            Set<Integer> others = uniqueRandomInts(userIds.size() - 1, properties.conversationPerUser());
            for (int idx : others) {
                int actualIdx = idx >= i ? idx + 1 : idx;
                UUID[] pair = sortedPair(user, userIds.get(actualIdx));
                if (seenPairs.add(new UuidPair(pair[0], pair[1]))) {
                    pairs.add(pair);
                }
            }
        }

        Map<UUID, UUID[]> conversations = new LinkedHashMap<>();
        List<Object[]> rows = new ArrayList<>();
        for (UUID[] pair : pairs) {
            UUID id = UUID.randomUUID();
            conversations.put(id, pair);
            Instant createdAt = randomBetween(twoWeeksAgo, now);
            rows.add(new Object[]{id, pair[0], pair[1], Timestamp.from(createdAt)});
        }

        parallelInsert(rows.size(), (offset, chunk) -> {
            List<Object[]> slice = rows.subList(offset, Math.min(offset + chunk, rows.size()));
            template.batchUpdate(SQL, slice);
        });

        log.info("Conversations generated: {}", conversations.size());
        return conversations;
    }

    // Postgres uuid 비교(부호 없는 바이트 단위)와 일치시키기 위한 unsigned 비교.
    // ck_conv_order 체크 제약과 동일한 기준 (com.codeit.team5.mopl.dm.util.UuidUtils와 동일 로직).
    private static UUID[] sortedPair(UUID a, UUID b) {
        int cmp = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        if (cmp == 0) {
            cmp = Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
        }
        return cmp <= 0 ? new UUID[]{a, b} : new UUID[]{b, a};
    }
}

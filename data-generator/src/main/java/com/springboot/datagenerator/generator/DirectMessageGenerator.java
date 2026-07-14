package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DirectMessageGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO direct_messages (id, conversation_id, sender_id, receiver_id, content, is_read, read_at, created_at) VALUES (?,?,?,?,?,?,?,?)";

    public DirectMessageGenerator(GeneratorProperties properties, JdbcTemplate template,
                                  @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    // 참여자 두 명이 번갈아 주고받는 것처럼 sender/receiver를 교대하고, 대화방마다 마지막 메시지만
    // 안읽음으로 남겨 "최신 메시지가 안읽음인" 케이스를 재현한다.
    public void run(Map<UUID, UUID[]> conversations) {
        log.info("Generating direct messages ({} conversations × {} each)...",
            conversations.size(), properties.messagePerConversation());

        List<UUID> conversationIds = new ArrayList<>(conversations.keySet());
        parallelInsert(conversationIds.size(), (offset, chunk) -> {
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = conversationIds.subList(offset, Math.min(offset + chunk, conversationIds.size()));

            for (UUID conversationId : slice) {
                UUID[] pair = conversations.get(conversationId);
                List<Instant> timestamps = new ArrayList<>();
                for (int i = 0; i < properties.messagePerConversation(); i++) {
                    timestamps.add(randomBetween(twoWeeksAgo, now));
                }
                timestamps.sort(Comparator.naturalOrder());

                for (int i = 0; i < timestamps.size(); i++) {
                    boolean fromParticipant1 = i % 2 == 0;
                    UUID sender = fromParticipant1 ? pair[0] : pair[1];
                    UUID receiver = fromParticipant1 ? pair[1] : pair[0];
                    Instant createdAt = timestamps.get(i);
                    boolean isRead = i < timestamps.size() - 1;
                    Timestamp readAt = isRead ? Timestamp.from(randomBetween(createdAt, now)) : null;

                    rows.add(new Object[]{
                        UUID.randomUUID(),
                        conversationId,
                        sender,
                        receiver,
                        faker.get().lorem().sentence(3, 10),
                        isRead,
                        readAt,
                        Timestamp.from(createdAt)
                    });
                }
            }
            template.batchUpdate(SQL, rows);
        });

        log.info("Direct messages generated");
    }
}

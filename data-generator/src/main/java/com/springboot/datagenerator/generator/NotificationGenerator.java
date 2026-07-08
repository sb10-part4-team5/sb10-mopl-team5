package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO notifications (id, receiver_id, type, title, content, level, is_read, read_at, created_at) VALUES (?,?,?,?,?,?,?,?,?)";

    private static final String[] TYPES = {
        "ROLE_CHANGED", "PLAYLIST_SUBSCRIBED", "PLAYLIST_UPDATED",
        "FOLLOWED", "DIRECT_MESSAGE", "WATCHING_ACTIVITY"
    };
    private static final String[] LEVELS = {"INFO", "INFO", "INFO", "WARNING", "ERROR"};

    public NotificationGenerator(GeneratorProperties properties, JdbcTemplate template,
                                 @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public void run(List<UUID> userIds) {
        log.info("Generating notifications ({} users × {} each)...", userIds.size(), properties.notificationPerUser());
        parallelInsert(userIds.size(), (offset, chunk) -> {
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = userIds.subList(offset, Math.min(offset + chunk, userIds.size()));
            for (UUID receiverId : slice) {
                for (int i = 0; i < properties.notificationPerUser(); i++) {
                    String type = TYPES[ThreadLocalRandom.current().nextInt(TYPES.length)];
                    String level = LEVELS[ThreadLocalRandom.current().nextInt(LEVELS.length)];
                    boolean isRead = ThreadLocalRandom.current().nextBoolean();
                    Instant createdAt = randomBetween(twoWeeksAgo, now);
                    Timestamp readAt = isRead ? Timestamp.from(randomBetween(createdAt, now)) : null;
                    rows.add(new Object[]{
                        UUID.randomUUID(),
                        receiverId,
                        type,
                        faker.get().lorem().sentence(3, 6),
                        faker.get().lorem().sentence(5, 10),
                        level,
                        isRead,
                        readAt,
                        Timestamp.from(createdAt)
                    });
                }
            }
            template.batchUpdate(SQL, rows);
        });
        log.info("Notifications generated");
    }
}

package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaylistGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO playlists (id, owner_id, title, description, subscriber_count, created_at, updated_at) VALUES (?,?,?,?,?,?,?)";

    public PlaylistGenerator(GeneratorProperties properties, JdbcTemplate template,
                             @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public List<UUID> run(List<UUID> userIds) {
        log.info("Generating playlists ({} users × {} each)...", userIds.size(), properties.playlistPerUser());
        List<UUID> ids = Collections.synchronizedList(new ArrayList<>());
        parallelInsert(userIds.size(), (offset, chunk) -> {
            List<UUID> chunkIds = new ArrayList<>();
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = userIds.subList(offset, Math.min(offset + chunk, userIds.size()));
            for (UUID ownerId : slice) {
                for (int j = 0; j < properties.playlistPerUser(); j++) {
                    UUID id = UUID.randomUUID();
                    chunkIds.add(id);
                    Instant createdAt = randomBetween(twoWeeksAgo, now);
                    rows.add(new Object[]{
                        id, ownerId,
                        faker.get().lorem().sentence(3, 5),
                        faker.get().lorem().paragraph(),
                        0,
                        Timestamp.from(createdAt),
                        Timestamp.from(createdAt)
                    });
                }
            }
            template.batchUpdate(SQL, rows);
            ids.addAll(chunkIds);
        });
        log.info("Playlists generated: {}", ids.size());
        return ids;
    }
}

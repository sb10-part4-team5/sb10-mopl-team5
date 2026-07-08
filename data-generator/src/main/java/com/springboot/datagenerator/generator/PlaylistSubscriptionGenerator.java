package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaylistSubscriptionGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO playlist_subscriptions (id, playlist_id, subscriber_id, created_at) VALUES (?,?,?,?)";

    public PlaylistSubscriptionGenerator(GeneratorProperties properties, JdbcTemplate template,
                                         @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public void run(List<UUID> userIds, List<UUID> playlistIds) {
        log.info("Generating playlist subscriptions ({} users × {} each)...", userIds.size(), properties.subscriptionPerUser());
        parallelInsert(userIds.size(), (offset, chunk) -> {
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = userIds.subList(offset, Math.min(offset + chunk, userIds.size()));
            for (UUID subscriberId : slice) {
                Set<Integer> indices = uniqueRandomInts(playlistIds.size(), properties.subscriptionPerUser());
                for (int idx : indices) {
                    rows.add(new Object[]{
                        UUID.randomUUID(),
                        playlistIds.get(idx),
                        subscriberId,
                        Timestamp.from(randomBetween(twoWeeksAgo, now))
                    });
                }
            }
            template.batchUpdate(SQL, rows);
        });
        log.info("Playlist subscriptions generated");
    }
}

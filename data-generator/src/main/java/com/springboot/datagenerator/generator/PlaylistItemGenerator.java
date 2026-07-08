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
public class PlaylistItemGenerator extends BaseGenerator {

    // added_at 은 @AttributeOverride(name = "createdAt") 로 매핑됨 (V17에서 position 컬럼 제거됨)
    private static final String SQL =
        "INSERT INTO playlist_items (id, playlist_id, content_id, added_at) VALUES (?,?,?,?)";

    public PlaylistItemGenerator(GeneratorProperties properties, JdbcTemplate template,
                                 @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public void run(List<UUID> playlistIds, List<UUID> contentIds) {
        log.info("Generating playlist items ({} playlists × {} items each)...", playlistIds.size(), properties.itemPerPlaylist());
        parallelInsert(playlistIds.size(), (offset, chunk) -> {
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = playlistIds.subList(offset, Math.min(offset + chunk, playlistIds.size()));
            for (UUID playlistId : slice) {
                Set<Integer> indices = uniqueRandomInts(contentIds.size(), properties.itemPerPlaylist());
                for (int idx : indices) {
                    rows.add(new Object[]{
                        UUID.randomUUID(),
                        playlistId,
                        contentIds.get(idx),
                        Timestamp.from(randomBetween(twoWeeksAgo, now))
                    });
                }
            }
            template.batchUpdate(SQL, rows);
        });
        log.info("Playlist items generated");
    }
}

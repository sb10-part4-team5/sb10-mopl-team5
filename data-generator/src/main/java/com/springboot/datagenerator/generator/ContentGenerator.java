package com.springboot.datagenerator.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentGenerator extends BaseGenerator {

    private static final String CONTENT_SQL =
        "INSERT INTO contents (id, type, title, description, released_at, metadata, source, external_id, created_at, updated_at) VALUES (?,?,?,?,?,CAST(? AS JSONB),?,?,?,?)";
    private static final String STATS_SQL =
        "INSERT INTO content_stats (id, review_count, rating_sum, watcher_count) VALUES (?,0,0,0)";

    private static final String[] CONTENT_TYPES = {"MOVIE", "TV_SERIES", "SPORT"};
    private static final String[] GENRES = {"Action", "Drama", "Comedy", "Romance", "Thriller", "Sci-Fi", "Horror", "Animation"};
    private static final String[] LANGUAGES = {"en", "ko", "ja", "fr", "de"};
    private static final String[] LEAGUES = {"Premier League", "La Liga", "Bundesliga", "Serie A", "K League"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContentGenerator(GeneratorProperties properties, JdbcTemplate template,
                            @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public List<UUID> run() {
        log.info("Generating {} contents...", properties.content());
        List<UUID> ids = parallel(properties.content(), chunk -> {
            List<UUID> chunkIds = new ArrayList<>();
            List<Object[]> contentRows = new ArrayList<>();
            List<Object[]> statsRows = new ArrayList<>();
            for (int i = 0; i < chunk; i++) {
                UUID id = UUID.randomUUID();
                chunkIds.add(id);
                String type = CONTENT_TYPES[ThreadLocalRandom.current().nextInt(CONTENT_TYPES.length)];
                String source = sourceFor(type);
                String metadata = metadataFor(type);
                Instant releasedAt = releasedAtFor(type);
                Instant createdAt = randomBetween(twoWeeksAgo, now);
                contentRows.add(new Object[]{
                    id, type,
                    faker.get().book().title(),
                    faker.get().lorem().characters(50, 500),
                    Timestamp.from(releasedAt),
                    metadata,
                    source,
                    UUID.randomUUID().toString(),
                    Timestamp.from(createdAt),
                    Timestamp.from(createdAt)
                });
                statsRows.add(new Object[]{id});
            }
            template.batchUpdate(CONTENT_SQL, contentRows);
            template.batchUpdate(STATS_SQL, statsRows);
            return chunkIds;
        });
        log.info("Contents generated: {}", ids.size());
        return ids;
    }

    private String sourceFor(String type) {
        return switch (type) {
            case "SPORT" -> "SPORTS_DB";
            default -> "TMDB";
        };
    }

    private Instant releasedAtFor(String type) {
        return switch (type) {
            case "SPORT" -> randomBetween(now, now.plus(30, ChronoUnit.DAYS));
            default -> randomBetween(now.minus(365, ChronoUnit.DAYS), now);
        };
    }

    private String metadataFor(String type) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            switch (type) {
                case "MOVIE" -> {
                    map.put("runtime", ThreadLocalRandom.current().nextInt(80, 200));
                    map.put("genres", randomGenres());
                    map.put("language", LANGUAGES[ThreadLocalRandom.current().nextInt(LANGUAGES.length)]);
                }
                case "TV_SERIES" -> {
                    map.put("runtime", 45);
                    map.put("genres", randomGenres());
                    map.put("language", LANGUAGES[ThreadLocalRandom.current().nextInt(LANGUAGES.length)]);
                    map.put("seasons", ThreadLocalRandom.current().nextInt(1, 10));
                }
                case "SPORT" -> {
                    map.put("league", LEAGUES[ThreadLocalRandom.current().nextInt(LEAGUES.length)]);
                    map.put("season", "2024-25");
                    map.put("homeTeam", faker.get().name().lastName() + " FC");
                    map.put("awayTeam", faker.get().name().lastName() + " FC");
                }
                default -> { return null; }
            }
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> randomGenres() {
        int count = ThreadLocalRandom.current().nextInt(1, 4);
        List<String> selected = new ArrayList<>();
        for (int idx : uniqueRandomInts(GENRES.length, count)) {
            selected.add(GENRES[idx]);
        }
        return selected;
    }
}

package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReviewGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO reviews (id, content_id, user_id, text, rating, created_at, updated_at) VALUES (?,?,?,?,?,?,?)";

    public ReviewGenerator(GeneratorProperties properties, JdbcTemplate template,
                           @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public void run(List<UUID> userIds, List<UUID> contentIds) {
        log.info("Generating reviews ({} users × {} reviews each)...", userIds.size(), properties.reviewPerUser());
        parallelInsert(userIds.size(), (offset, chunk) -> {
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = userIds.subList(offset, Math.min(offset + chunk, userIds.size()));
            for (UUID userId : slice) {
                Set<Integer> indices = uniqueRandomInts(contentIds.size(), properties.reviewPerUser());
                for (int idx : indices) {
                    Instant createdAt = randomBetween(twoWeeksAgo, now);
                    double rating = Math.round(ThreadLocalRandom.current().nextDouble(1.0, 5.01) * 2) / 2.0;
                    rows.add(new Object[]{
                        UUID.randomUUID(),
                        contentIds.get(idx),
                        userId,
                        faker.get().lorem().paragraph(2),
                        rating,
                        Timestamp.from(createdAt),
                        Timestamp.from(createdAt)
                    });
                }
            }
            template.batchUpdate(SQL, rows);
        });
        log.info("Reviews generated");
    }
}

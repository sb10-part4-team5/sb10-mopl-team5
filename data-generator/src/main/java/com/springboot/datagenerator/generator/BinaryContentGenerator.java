package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BinaryContentGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO binary_contents (id, url, upload_status, created_at, updated_at) VALUES (?,?,?,?,?)";

    public BinaryContentGenerator(GeneratorProperties properties, JdbcTemplate template,
                                  @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public List<UUID> run(int count) {
        log.info("Generating {} binary contents...", count);
        List<UUID> ids = parallel(count, chunk -> {
            List<UUID> chunkIds = new ArrayList<>();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < chunk; i++) {
                UUID id = UUID.randomUUID();
                chunkIds.add(id);
                Instant createdAt = randomBetween(twoWeeksAgo, now);
                rows.add(new Object[]{
                    id,
                    "https://cdn.mopl-dev.site/profile/" + id + ".jpg",
                    "COMPLETED",
                    Timestamp.from(createdAt),
                    Timestamp.from(createdAt)
                });
            }
            template.batchUpdate(SQL, rows);
            return chunkIds;
        });
        log.info("Binary contents generated: {}", ids.size());
        return ids;
    }
}

package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO users (id, email, password, name, role, locked, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)";

    public UserGenerator(GeneratorProperties properties, JdbcTemplate template,
                         @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    private final AtomicInteger counter = new AtomicInteger(1);

    public List<UUID> run() {
        log.info("Generating {} users...", properties.user());
        List<UUID> ids = parallel(properties.user(), chunk -> {
            List<UUID> chunkIds = new ArrayList<>();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < chunk; i++) {
                UUID id = UUID.randomUUID();
                chunkIds.add(id);
                Instant createdAt = randomBetween(twoWeeksAgo, now);
                rows.add(new Object[]{
                    id,
                    "user" + counter.getAndIncrement() + "@" + faker.get().internet().domainName(),
                    faker.get().internet().password(8, 20),
                    faker.get().name().fullName(),
                    "USER",
                    false,
                    Timestamp.from(createdAt),
                    Timestamp.from(createdAt)
                });
            }
            template.batchUpdate(SQL, rows);
            return chunkIds;
        });
        log.info("Users generated: {}", ids.size());
        return ids;
    }
}

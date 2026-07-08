package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
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
public class TagGenerator extends BaseGenerator {

    private static final String SQL = "INSERT INTO tags (id, name) VALUES (?,?)";

    public TagGenerator(GeneratorProperties properties, JdbcTemplate template,
                        @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public List<UUID> run() {
        log.info("Generating {} tags...", properties.tag());
        List<UUID> ids = parallel(properties.tag(), chunk -> {
            List<UUID> chunkIds = new ArrayList<>();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < chunk; i++) {
                UUID id = UUID.randomUUID();
                chunkIds.add(id);
                String name = faker.get().lorem().word() + "_" + UUID.randomUUID().toString().substring(0, 8);
                rows.add(new Object[]{id, name});
            }
            template.batchUpdate(SQL, rows);
            return chunkIds;
        });
        log.info("Tags generated: {}", ids.size());
        return ids;
    }
}

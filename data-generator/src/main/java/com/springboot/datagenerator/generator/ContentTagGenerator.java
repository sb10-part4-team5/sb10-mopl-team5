package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
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
public class ContentTagGenerator extends BaseGenerator {

    private static final String SQL = "INSERT INTO content_tags (content_id, tag_id) VALUES (?,?)";

    public ContentTagGenerator(GeneratorProperties properties, JdbcTemplate template,
                               @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public void run(List<UUID> contentIds, List<UUID> tagIds) {
        log.info("Generating content tags ({} contents × {} tags each)...", contentIds.size(), properties.tagPerContent());
        parallelInsert(contentIds.size(), (offset, chunk) -> {
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = contentIds.subList(offset, Math.min(offset + chunk, contentIds.size()));
            for (UUID contentId : slice) {
                Set<Integer> indices = uniqueRandomInts(tagIds.size(), properties.tagPerContent());
                for (int idx : indices) {
                    rows.add(new Object[]{contentId, tagIds.get(idx)});
                }
            }
            template.batchUpdate(SQL, rows);
        });
        log.info("Content tags generated");
    }
}

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
public class FollowGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO follows (id, follower_id, followee_id, created_at) VALUES (?,?,?,?)";

    public FollowGenerator(GeneratorProperties properties, JdbcTemplate template,
                           @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public void run(List<UUID> userIds) {
        log.info("Generating follows ({} users × {} each)...", userIds.size(), properties.followPerUser());
        parallelInsert(userIds.size(), (offset, chunk) -> {
            List<Object[]> rows = new ArrayList<>();
            List<UUID> slice = userIds.subList(offset, Math.min(offset + chunk, userIds.size()));
            for (int i = 0; i < slice.size(); i++) {
                UUID followerId = slice.get(i);
                int followerGlobalIdx = offset + i;
                // 자기 자신 제외 (ck_follow_self 제약)
                Set<Integer> indices = uniqueRandomInts(userIds.size() - 1, properties.followPerUser());
                for (int idx : indices) {
                    int actualIdx = idx >= followerGlobalIdx ? idx + 1 : idx;
                    rows.add(new Object[]{
                        UUID.randomUUID(),
                        followerId,
                        userIds.get(actualIdx),
                        Timestamp.from(randomBetween(twoWeeksAgo, now))
                    });
                }
            }
            template.batchUpdate(SQL, rows);
        });
        log.info("Follows generated");
    }
}

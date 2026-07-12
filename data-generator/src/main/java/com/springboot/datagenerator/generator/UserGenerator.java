package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserGenerator extends BaseGenerator {

    private static final String SQL =
        "INSERT INTO users (id, email, password, name, role, locked, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)";

    // 부하테스트용 고정 비밀번호 - 미리 해시해서 재사용 (BCrypt는 건당 ~100ms라 매번 호출 시 성능 저하)
    static final String TEST_PASSWORD = "Test1234!";
    private static final String HASHED_PASSWORD =
        new BCryptPasswordEncoder().encode(TEST_PASSWORD);

    private final AtomicInteger counter = new AtomicInteger(1);

    public UserGenerator(GeneratorProperties properties, JdbcTemplate template,
                         @Qualifier("jdbcWorker") Executor executor) {
        super(properties, template, executor);
    }

    public List<UUID> run() {
        log.info("Generating {} users... (password: {})", properties.user(), TEST_PASSWORD);
        List<UUID> ids = parallel(properties.user(), chunk -> {
            List<UUID> chunkIds = new ArrayList<>();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < chunk; i++) {
                UUID id = UUID.randomUUID();
                chunkIds.add(id);
                Instant createdAt = randomBetween(twoWeeksAgo, now);
                rows.add(new Object[]{
                    id,
                    // 부하테스트에서 k6가 DB 조회 없이 로그인할 수 있도록 이메일을 순번 고정으로 생성
                    // (user1@loadtest.local ~ user{N}@loadtest.local). counter가 유일성 보장.
                    "user" + counter.getAndIncrement() + "@loadtest.local",
                    HASHED_PASSWORD,
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

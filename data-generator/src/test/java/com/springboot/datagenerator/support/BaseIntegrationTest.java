package com.springboot.datagenerator.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withInitScript("schema.sql");

    @Autowired
    protected JdbcTemplate template;

    @BeforeEach
    void truncateAll() {
        template.execute("""
            TRUNCATE TABLE
                follows, playlist_subscriptions, playlist_items,
                content_tags, reviews, notifications,
                playlists, content_stats, contents, tags, users
            RESTART IDENTITY CASCADE
            """);
    }
}

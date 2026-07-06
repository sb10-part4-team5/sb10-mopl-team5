ALTER TABLE contents
    DROP COLUMN IF EXISTS stats_id;

DROP TABLE IF EXISTS content_stats;

CREATE TABLE content_stats
(
    id            UUID             PRIMARY KEY REFERENCES contents (id) ON DELETE CASCADE,
    review_count  INTEGER          NOT NULL DEFAULT 0,
    rating_sum    DOUBLE PRECISION NOT NULL DEFAULT 0,
    watcher_count BIGINT           NOT NULL DEFAULT 0
);

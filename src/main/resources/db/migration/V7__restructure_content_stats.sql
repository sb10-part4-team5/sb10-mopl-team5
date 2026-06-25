ALTER TABLE contents
    DROP COLUMN IF EXISTS stats_id;

DROP TABLE IF EXISTS content_stats;

CREATE TABLE content_stats
(
    id         UUID PRIMARY KEY,
    review_count  INTEGER          NOT NULL DEFAULT 0,
    rating_sum    DOUBLE PRECISION NOT NULL DEFAULT 0,
    watcher_count INTEGER          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

ALTER TABLE contents
    ADD COLUMN stats_id UUID
        CONSTRAINT fk_contents_stats REFERENCES content_stats (id) ON DELETE SET NULL;

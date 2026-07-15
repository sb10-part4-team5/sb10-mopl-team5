CREATE INDEX idx_contents_created_at ON contents (created_at);
CREATE INDEX idx_content_stats_watcher_count ON content_stats (watcher_count DESC, id DESC);

ALTER TABLE content_stats ADD COLUMN average_rating DOUBLE PRECISION NOT NULL DEFAULT 0;

UPDATE content_stats
SET average_rating = CASE WHEN review_count = 0 THEN 0 ELSE rating_sum / review_count END;

CREATE INDEX idx_content_stats_average_rating ON content_stats (average_rating DESC, id DESC);

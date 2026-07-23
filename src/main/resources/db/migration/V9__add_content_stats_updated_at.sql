ALTER TABLE content_stats
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

CREATE INDEX idx_content_stats_updated_at ON content_stats (updated_at);

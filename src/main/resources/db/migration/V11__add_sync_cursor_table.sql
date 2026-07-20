CREATE TABLE sync_cursor (
    name VARCHAR(64) NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (name)
);

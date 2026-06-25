CREATE TABLE binary_contents
(
    id            UUID PRIMARY KEY,
    url           VARCHAR(512) NOT NULL,
    upload_status VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_binary_contents_upload_status CHECK (upload_status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

ALTER TABLE contents
    ADD COLUMN thumbnail_id UUID
        CONSTRAINT fk_contents_thumbnail REFERENCES binary_contents (id) ON DELETE SET NULL;

ALTER TABLE contents
    DROP COLUMN thumbnail_url,
    DROP COLUMN thumbnail_upload_status;

CREATE TABLE binary_contents
(
    id            UUID PRIMARY KEY,
    url           VARCHAR(512) NOT NULL,
    upload_status VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_binary_contents_upload_status CHECK (upload_status IN ('COMPLETED', 'DELETED'))
);

ALTER TABLE contents
    ADD CONSTRAINT fk_contents_thumbnail FOREIGN KEY (thumbnail_id) REFERENCES binary_contents (id) ON DELETE SET NULL,
    ADD CONSTRAINT uk_contents_thumbnail_id UNIQUE (thumbnail_id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_profile_image FOREIGN KEY (profile_image_id) REFERENCES binary_contents (id) ON DELETE SET NULL,
    ADD CONSTRAINT uk_users_profile_image_id UNIQUE (profile_image_id);

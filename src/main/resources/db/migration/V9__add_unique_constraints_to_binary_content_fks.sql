ALTER TABLE contents
    ADD CONSTRAINT uk_contents_thumbnail_id UNIQUE (thumbnail_id);

ALTER TABLE users
    ADD CONSTRAINT uk_users_profile_image_id UNIQUE (profile_image_id);
